package com.agricultural.assistplatform.service.user;

import cn.hutool.core.util.IdUtil;
import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.dto.user.CreateOrderDTO;
import com.agricultural.assistplatform.entity.*;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.*;
import com.agricultural.assistplatform.service.common.MerchantRealtimeEventService;
import com.agricultural.assistplatform.service.common.AdminRealtimeEventService;
import com.agricultural.assistplatform.service.common.PublicDataCacheService;
import com.agricultural.assistplatform.service.common.UserRealtimeEventService;
import com.agricultural.assistplatform.vo.user.OrderItemVO;
import com.agricultural.assistplatform.vo.user.OrderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户端订单核心服务。
 * 这里维护了下单、取消、支付、确认收货、库存扣减/回补以及订单状态流转。
 *
 * 订单状态 1-待付款 2-待发货 3-待收货 4-已完成 5-已取消 6-支付异常 7-售后中 8-已完成售后
 */
@Service
@RequiredArgsConstructor
public class UserOrderService {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductInfoMapper productInfoMapper;
    private final UserAddressMapper userAddressMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final LogisticsInfoMapper logisticsInfoMapper;
    private final MerchantRealtimeEventService merchantRealtimeEventService;
    private final UserRealtimeEventService userRealtimeEventService;
    private final AdminRealtimeEventService adminRealtimeEventService;
    private final PublicDataCacheService publicDataCacheService;

    private static final int PAY_DEADLINE_MINUTES = 15;

    /**
     * 创建订单主流程。
     * 先校验地址和商品，再原子扣减库存，最后写入订单主表、子表并广播实时刷新事件。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(CreateOrderDTO dto) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        UserAddress addr = userAddressMapper.selectById(dto.getAddressId());
        if (addr == null || !addr.getUserId().equals(userId))
            throw new BusinessException(ResultCode.BAD_REQUEST, "收货地址无效");
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Long merchantId = null;
        if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
            List<Cart> carts = cartMapper.selectBatchIds(dto.getCartIds());
            for (Cart c : carts) {
                if (!c.getUserId().equals(userId)) continue;
                ProductInfo p = productInfoMapper.selectById(c.getProductId());
                if (p == null || p.getStatus() != 1) throw new BusinessException(ResultCode.BAD_REQUEST, "商品已下架");
                merchantId = ensureSameMerchant(merchantId, p.getMerchantId());
                int stock = p.getStock() != null ? p.getStock() : 0;
                if (stock < c.getProductNum()) throw new BusinessException(ResultCode.BAD_REQUEST, "当前仅 " + stock + " 件可购");
                BigDecimal amount = p.getPrice().multiply(BigDecimal.valueOf(c.getProductNum()));
                total = total.add(amount);
                OrderItem oi = new OrderItem();
                oi.setOrderNo(null);
                oi.setProductId(p.getId());
                oi.setProductName(p.getProductName());
                oi.setProductImg(p.getProductImg());
                oi.setProductPrice(p.getPrice());
                oi.setProductNum(c.getProductNum());
                oi.setProductAmount(amount);
                items.add(oi);
            }
        } else if (dto.getProductItems() != null && !dto.getProductItems().isEmpty()) {
            for (CreateOrderDTO.OrderProductItem pi : dto.getProductItems()) {
                ProductInfo p = productInfoMapper.selectById(pi.getProductId());
                if (p == null || p.getStatus() != 1) throw new BusinessException(ResultCode.BAD_REQUEST, "商品已下架");
                merchantId = ensureSameMerchant(merchantId, p.getMerchantId());
                int num = (pi.getProductNum() != null && pi.getProductNum() > 0) ? pi.getProductNum() : 1;
                int stock = p.getStock() != null ? p.getStock() : 0;
                if (stock < num) throw new BusinessException(ResultCode.BAD_REQUEST, "当前仅 " + stock + " 件可购");
                BigDecimal amount = p.getPrice().multiply(BigDecimal.valueOf(num));
                total = total.add(amount);
                OrderItem oi = new OrderItem();
                oi.setOrderNo(null);
                oi.setProductId(p.getId());
                oi.setProductName(p.getProductName());
                oi.setProductImg(p.getProductImg());
                oi.setProductPrice(p.getPrice());
                oi.setProductNum(num);
                oi.setProductAmount(amount);
                items.add(oi);
            }
        }
        if (items.isEmpty() || merchantId == null) throw new BusinessException(ResultCode.BAD_REQUEST, "请选择商品");
        String orderNo = generateUniqueOrderNo();
        OrderMain main = new OrderMain();
        main.setOrderNo(orderNo);
        main.setUserId(userId);
        main.setAddressId(addr.getId());
        main.setTotalAmount(total);
        main.setOrderStatus(1);
        main.setPayDeadline(LocalDateTime.now().plusMinutes(PAY_DEADLINE_MINUTES));
        main.setReceiver(addr.getReceiver());
        main.setReceiverPhone(addr.getPhone());
        main.setReceiverAddress(addr.getProvince() + addr.getCity() + addr.getCounty() + addr.getTown() + addr.getDetailAddress());
        main.setRemark(dto.getRemark());
        main.setMerchantId(merchantId);
        orderMainMapper.insert(main);
        for (OrderItem oi : items) {
            oi.setOrderNo(orderNo);
            orderItemMapper.insert(oi);
            int updated = productInfoMapper.deductStockSafely(oi.getProductId(), oi.getProductNum());
            if (updated < 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "商品库存不足，请刷新后重试");
            }
            refreshProductCache(oi.getProductId(), merchantId);
        }
        if (dto.getCartIds() != null) cartMapper.delete(new LambdaQueryWrapper<Cart>().in(Cart::getId, dto.getCartIds()).eq(Cart::getUserId, userId));
        userRealtimeEventService.publishRefresh(userId, "ORDER_CREATED", orderNo);
        userRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", orderNo);
        adminRealtimeEventService.publishRefreshToAll("ORDER_CREATED", orderNo);
        adminRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", orderNo);
        return getByOrderNo(orderNo);
    }

    /**
     * 订单号全局唯一，避免极端并发下同毫秒重复导致同单号多记录问题
     */
    private String generateUniqueOrderNo() {
        while (true) {
            String orderNo = "O" + IdUtil.getSnowflakeNextIdStr();
            Long exists = orderMainMapper.selectCount(new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getOrderNo, orderNo));
            if (Objects.equals(exists, 0L)) {
                return orderNo;
            }
        }
    }

    public PageResult<OrderVO> list(Integer orderStatus, Integer pageNum, Integer pageSize) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<OrderMain> q = new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getUserId, userId);
        if (orderStatus != null) {
            if (orderStatus == 4) {
                q.in(OrderMain::getOrderStatus, 4, 8);
            } else {
                q.eq(OrderMain::getOrderStatus, orderStatus);
            }
        }
        q.orderByDesc(OrderMain::getCreateTime);
        Page<OrderMain> page = orderMainMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<OrderVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), list);
    }

    public OrderVO getById(Long id) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getId, id).eq(OrderMain::getUserId, userId));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        return toVO(main);
    }

    private OrderVO getByOrderNo(String orderNo) {
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getOrderNo, orderNo));
        return main != null ? toVO(main) : null;
    }

    /**
     * 用户手动取消订单。
     * 只允许取消待付款订单，并在成功后回补库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getId, id).eq(OrderMain::getUserId, userId));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (main.getOrderStatus() != 1) throw new BusinessException(ResultCode.BAD_REQUEST, "仅待付款订单可取消");
        int updated = orderMainMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderMain>()
                .eq(OrderMain::getId, main.getId())
                .eq(OrderMain::getOrderStatus, 1)
                .set(OrderMain::getOrderStatus, 5)
                .set(OrderMain::getCancelReason, "用户手动取消"));
        if (updated < 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态已变更，请刷新后重试");
        }
        releaseOrderStock(main);
        userRealtimeEventService.publishRefresh(userId, "ORDER_CANCELED", main.getOrderNo());
        userRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", main.getOrderNo());
        adminRealtimeEventService.publishRefreshToAll("ORDER_CANCELED", main.getOrderNo());
        adminRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", main.getOrderNo());
    }

    /**
     * 模拟支付流程。
     * 这里重点做了订单状态条件更新和支付记录幂等处理，避免重复支付写出脏数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id, Boolean success) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getId, id).eq(OrderMain::getUserId, userId));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        PaymentRecord existingPayment = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderNo, main.getOrderNo())
                .orderByDesc(PaymentRecord::getId)
                .last("LIMIT 1"));
        if (Boolean.TRUE.equals(success)
                && Integer.valueOf(2).equals(main.getOrderStatus())
                && existingPayment != null
                && Integer.valueOf(1).equals(existingPayment.getPayStatus())) {
            return;
        }
        if (main.getOrderStatus() != 1 && main.getOrderStatus() != 6) throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态不允许支付");
        if (main.getPayDeadline() != null && main.getPayDeadline().isBefore(LocalDateTime.now())) {
            int updated = orderMainMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderMain>()
                    .eq(OrderMain::getId, main.getId())
                    .in(OrderMain::getOrderStatus, 1, 6)
                    .lt(OrderMain::getPayDeadline, LocalDateTime.now())
                    .set(OrderMain::getOrderStatus, 5)
                    .set(OrderMain::getCancelReason, "超时未付款"));
            if (updated > 0) {
                releaseOrderStock(main);
            }
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单已超时取消");
        }
        
        if (!success) {
            orderMainMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderMain>()
                    .eq(OrderMain::getId, main.getId())
                    .eq(OrderMain::getUserId, userId)
                    .in(OrderMain::getOrderStatus, 1, 6)
                    .set(OrderMain::getOrderStatus, 6));
            throw new BusinessException(ResultCode.SERVER_ERROR, "支付异常，请重新支付");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = orderMainMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderMain>()
                .eq(OrderMain::getId, main.getId())
                .eq(OrderMain::getUserId, userId)
                .in(OrderMain::getOrderStatus, 1, 6)
                .and(wrapper -> wrapper.isNull(OrderMain::getPayDeadline).or().ge(OrderMain::getPayDeadline, now))
                .set(OrderMain::getOrderStatus, 2)
                .set(OrderMain::getCancelReason, null));
        if (updated < 1) {
            OrderMain latest = orderMainMapper.selectById(main.getId());
            PaymentRecord latestPayment = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                    .eq(PaymentRecord::getOrderNo, main.getOrderNo())
                    .orderByDesc(PaymentRecord::getId)
                    .last("LIMIT 1"));
            if (latest != null
                    && Integer.valueOf(2).equals(latest.getOrderStatus())
                    && latestPayment != null
                    && Integer.valueOf(1).equals(latestPayment.getPayStatus())) {
                return;
            }
            if (latest != null && Integer.valueOf(5).equals(latest.getOrderStatus())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "订单已取消，无法继续支付");
            }
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态已变更，请刷新后重试");
        }
        if (existingPayment == null) {
            PaymentRecord pr = new PaymentRecord();
            pr.setOrderNo(main.getOrderNo());
            pr.setPayAmount(main.getTotalAmount());
            pr.setPayStatus(1);
            pr.setPayTime(now);
            pr.setPayType(1);
            paymentRecordMapper.insert(pr);
        }
        userRealtimeEventService.publishRefresh(userId, "ORDER_PAID", main.getOrderNo());
        merchantRealtimeEventService.publishRefresh(main.getMerchantId(), "ORDER_PAID", main.getOrderNo());
        adminRealtimeEventService.publishRefreshToAll("ORDER_PAID", main.getOrderNo());
    }

    /**
     * 确认收货，把订单从待收货流转到已完成。
     */
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long id) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getId, id).eq(OrderMain::getUserId, userId));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (main.getOrderStatus() != 3) throw new BusinessException(ResultCode.BAD_REQUEST, "仅待收货订单可确认收货");
        main.setOrderStatus(4);
        orderMainMapper.updateById(main);
        merchantRealtimeEventService.publishRefresh(main.getMerchantId(), "ORDER_RECEIVED", main.getOrderNo());
        userRealtimeEventService.publishRefresh(userId, "ORDER_RECEIVED", main.getOrderNo());
        adminRealtimeEventService.publishRefreshToAll("ORDER_RECEIVED", main.getOrderNo());
    }

    public LogisticsInfo logistics(Long id) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getId, id).eq(OrderMain::getUserId, userId));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        return logisticsInfoMapper.selectOne(new LambdaQueryWrapper<LogisticsInfo>().eq(LogisticsInfo::getOrderNo, main.getOrderNo()));
    }

    private OrderVO toVO(OrderMain main) {
        OrderVO vo = new OrderVO();
        vo.setId(main.getId());
        vo.setOrderNo(main.getOrderNo());
        vo.setTotalAmount(main.getTotalAmount());
        vo.setOrderStatus(main.getOrderStatus());
        vo.setCancelReason(main.getCancelReason());
        vo.setPayDeadline(main.getPayDeadline());
        vo.setReceiver(main.getReceiver());
        vo.setReceiverPhone(main.getReceiverPhone());
        vo.setReceiverAddress(main.getReceiverAddress());
        vo.setRemark(main.getRemark());
        vo.setMerchantId(main.getMerchantId());
        vo.setCreateTime(main.getCreateTime());
        LogisticsInfo log = logisticsInfoMapper.selectOne(new LambdaQueryWrapper<LogisticsInfo>().eq(LogisticsInfo::getOrderNo, main.getOrderNo()));
        if (log != null) {
            vo.setLogisticsCompany(log.getLogisticsCompany());
            vo.setLogisticsNo(log.getLogisticsNo());
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, main.getOrderNo()));
        vo.setItems(items.stream().map(oi -> {
            OrderItemVO iv = new OrderItemVO();
            iv.setProductId(oi.getProductId());
            iv.setProductName(oi.getProductName());
            iv.setProductImg(oi.getProductImg());
            iv.setProductPrice(oi.getProductPrice());
            iv.setProductNum(oi.getProductNum());
            iv.setProductAmount(oi.getProductAmount());
            return iv;
        }).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 刷新商品相关缓存，避免库存或状态变化后用户侧仍看到旧数据。
     */
    private void refreshProductCache(Long productId, Long merchantId) {
        ProductInfo product = productInfoMapper.selectById(productId);
        publicDataCacheService.refreshHotProduct(product);
        publicDataCacheService.evictProductCatalog(productId, merchantId);
    }

    private Long ensureSameMerchant(Long currentMerchantId, Long nextMerchantId) {
        if (currentMerchantId == null) {
            return nextMerchantId;
        }
        if (!Objects.equals(currentMerchantId, nextMerchantId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前系统暂不支持跨商家合并下单，请按店铺分别提交订单");
        }
        return currentMerchantId;
    }

    /**
     * 回补取消订单的库存，并同步更新商品缓存。
     */
    private void releaseOrderStock(OrderMain main) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, main.getOrderNo()));
        for (OrderItem orderItem : orderItems) {
            productInfoMapper.restoreStockSafely(orderItem.getProductId(), orderItem.getProductNum());
            refreshProductCache(orderItem.getProductId(), main.getMerchantId());
        }
    }
}
