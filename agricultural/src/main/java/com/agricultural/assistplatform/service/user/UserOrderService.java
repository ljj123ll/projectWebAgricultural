package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.dto.user.CreateOrderDTO;
import com.agricultural.assistplatform.entity.*;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.*;
import com.agricultural.assistplatform.vo.user.OrderItemVO;
import com.agricultural.assistplatform.vo.user.OrderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单状态 1-待付款 2-待发货 3-待收货 4-已完成 5-已取消 6-支付异常
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

    private static final int PAY_DEADLINE_MINUTES = 15;

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
                int stock = p.getStock() != null ? p.getStock() : 0;
                if (stock < c.getProductNum()) throw new BusinessException(ResultCode.BAD_REQUEST, "当前仅 " + stock + " 件可购");
                merchantId = p.getMerchantId();
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
                int num = (pi.getProductNum() != null && pi.getProductNum() > 0) ? pi.getProductNum() : 1;
                int stock = p.getStock() != null ? p.getStock() : 0;
                if (stock < num) throw new BusinessException(ResultCode.BAD_REQUEST, "当前仅 " + stock + " 件可购");
                merchantId = p.getMerchantId();
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
        String orderNo = "O" + System.currentTimeMillis() + (userId % 10000);
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
        main.setMerchantId(merchantId);
        orderMainMapper.insert(main);
        for (OrderItem oi : items) {
            oi.setOrderNo(orderNo);
            orderItemMapper.insert(oi);
            productInfoMapper.update(null, new LambdaUpdateWrapper<ProductInfo>()
                    .eq(ProductInfo::getId, oi.getProductId())
                    .setSql("stock = stock - " + oi.getProductNum())
                    .setSql("sales_volume = sales_volume + " + oi.getProductNum()));
        }
        if (dto.getCartIds() != null) cartMapper.delete(new LambdaQueryWrapper<Cart>().in(Cart::getId, dto.getCartIds()).eq(Cart::getUserId, userId));
        return getByOrderNo(orderNo);
    }

    public PageResult<OrderVO> list(Integer orderStatus, Integer pageNum, Integer pageSize) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<OrderMain> q = new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getUserId, userId);
        if (orderStatus != null) q.eq(OrderMain::getOrderStatus, orderStatus);
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

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getId, id).eq(OrderMain::getUserId, userId));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (main.getOrderStatus() != 1) throw new BusinessException(ResultCode.BAD_REQUEST, "仅待付款订单可取消");
        main.setOrderStatus(5);
        main.setCancelReason("用户手动取消");
        orderMainMapper.updateById(main);
        for (OrderItem oi : orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, main.getOrderNo()))) {
            productInfoMapper.update(null, new LambdaUpdateWrapper<ProductInfo>()
                    .eq(ProductInfo::getId, oi.getProductId())
                    .setSql("stock = stock + " + oi.getProductNum()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getId, id).eq(OrderMain::getUserId, userId));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (main.getOrderStatus() != 1) throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态不允许支付");
        if (main.getPayDeadline() != null && main.getPayDeadline().isBefore(LocalDateTime.now())) {
            main.setOrderStatus(5);
            main.setCancelReason("超时未付款");
            orderMainMapper.updateById(main);
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单已超时取消");
        }
        main.setOrderStatus(2);
        orderMainMapper.updateById(main);
        PaymentRecord pr = new PaymentRecord();
        pr.setOrderNo(main.getOrderNo());
        pr.setPayAmount(main.getTotalAmount());
        pr.setPayStatus(1);
        pr.setPayTime(LocalDateTime.now());
        pr.setPayType(1);
        paymentRecordMapper.insert(pr);
    }

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
}
