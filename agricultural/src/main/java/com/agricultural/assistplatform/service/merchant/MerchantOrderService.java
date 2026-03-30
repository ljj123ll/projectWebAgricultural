package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.LogisticsInfo;
import com.agricultural.assistplatform.entity.OrderItem;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.LogisticsInfoMapper;
import com.agricultural.assistplatform.mapper.OrderItemMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.service.common.MerchantRealtimeEventService;
import com.agricultural.assistplatform.service.common.AdminRealtimeEventService;
import com.agricultural.assistplatform.service.common.PublicDataCacheService;
import com.agricultural.assistplatform.service.common.UserMessageService;
import com.agricultural.assistplatform.service.common.UserRealtimeEventService;
import com.agricultural.assistplatform.vo.merchant.MerchantOrderVO;
import com.agricultural.assistplatform.vo.user.OrderItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 商家订单服务。
 * 核心负责商家订单列表、发货、取消待发货订单和履约消息通知。
 */
public class MerchantOrderService {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final LogisticsInfoMapper logisticsInfoMapper;
    private final ProductInfoMapper productInfoMapper;
    private final MerchantRealtimeEventService merchantRealtimeEventService;
    private final AdminRealtimeEventService adminRealtimeEventService;
    private final UserRealtimeEventService userRealtimeEventService;
    private final UserMessageService userMessageService;
    private final PublicDataCacheService publicDataCacheService;

    public com.agricultural.assistplatform.common.PageResult<MerchantOrderVO> list(Integer orderStatus, Integer pageNum, Integer pageSize) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<OrderMain> q = new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getMerchantId, merchantId);
        if (orderStatus != null) {
            if (orderStatus == 4) {
                q.in(OrderMain::getOrderStatus, 4, 8);
            } else {
                q.eq(OrderMain::getOrderStatus, orderStatus);
            }
        }
        q.orderByDesc(OrderMain::getCreateTime);
        Page<OrderMain> page = orderMainMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<MerchantOrderVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), list);
    }

    public MerchantOrderVO get(Long id) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getId, id).eq(OrderMain::getMerchantId, merchantId));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        return toVO(main);
    }

    public MerchantOrderVO getByOrderNo(String orderNo) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (orderNo == null || orderNo.isBlank()) throw new BusinessException(ResultCode.BAD_REQUEST, "订单号不能为空");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, orderNo)
                .eq(OrderMain::getMerchantId, merchantId)
                .last("LIMIT 1"));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        return toVO(main);
    }

    /**
     * 商家发货入口。
     * 校验订单状态后写入物流信息，并把订单推进到待收货。
     */
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id, Map<String, String> body) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getId, id).eq(OrderMain::getMerchantId, merchantId));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (main.getOrderStatus() != 2) throw new BusinessException(ResultCode.BAD_REQUEST, "仅待发货订单可填写物流");
        String company = body.get("logisticsCompany");
        String no = body.get("logisticsNo");
        if (company == null || no == null || no.isBlank()) throw new BusinessException(ResultCode.BAD_REQUEST, "请填写快递公司与物流单号");
        main.setOrderStatus(3);
        orderMainMapper.updateById(main);
        LogisticsInfo log = logisticsInfoMapper.selectOne(new LambdaQueryWrapper<LogisticsInfo>().eq(LogisticsInfo::getOrderNo, main.getOrderNo()));
        if (log != null) {
            log.setLogisticsCompany(company);
            log.setLogisticsNo(no);
            log.setLogisticsStatus(1);
            logisticsInfoMapper.updateById(log);
        } else {
            LogisticsInfo newLog = new LogisticsInfo();
            newLog.setOrderNo(main.getOrderNo());
            newLog.setLogisticsCompany(company);
            newLog.setLogisticsNo(no);
            newLog.setLogisticsStatus(1);
            logisticsInfoMapper.insert(newLog);
        }
        merchantRealtimeEventService.publishRefresh(merchantId, "ORDER_SHIPPED", main.getOrderNo());
        userRealtimeEventService.publishRefresh(main.getUserId(), "ORDER_SHIPPED", main.getOrderNo());
        adminRealtimeEventService.publishRefreshToAll("ORDER_SHIPPED", main.getOrderNo());
        userMessageService.createMerchantMessage(
                main.getUserId(),
                "订单已发货",
                "您的订单 " + main.getOrderNo() + " 已由商家发货，请留意物流信息。",
                "order",
                main.getOrderNo()
        );
    }

    /**
     * 商家取消待发货订单，并回补库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getId, id).eq(OrderMain::getMerchantId, merchantId));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (main.getOrderStatus() != 2) throw new BusinessException(ResultCode.BAD_REQUEST, "仅待发货订单可取消");
        int updated = orderMainMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderMain>()
                .eq(OrderMain::getId, main.getId())
                .eq(OrderMain::getOrderStatus, 2)
                .set(OrderMain::getOrderStatus, 5)
                .set(OrderMain::getCancelReason, reason));
        if (updated < 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态已变更，请刷新后重试");
        }
        restoreOrderStock(main);
        merchantRealtimeEventService.publishRefresh(merchantId, "ORDER_CANCELED", main.getOrderNo());
        userRealtimeEventService.publishRefresh(main.getUserId(), "ORDER_CANCELED", main.getOrderNo());
        adminRealtimeEventService.publishRefreshToAll("ORDER_CANCELED", main.getOrderNo());
    }

    private MerchantOrderVO toVO(OrderMain main) {
        MerchantOrderVO vo = new MerchantOrderVO();
        vo.setId(main.getId());
        vo.setOrderNo(main.getOrderNo());
        vo.setTotalAmount(main.getTotalAmount());
        vo.setOrderStatus(main.getOrderStatus());
        vo.setReceiver(main.getReceiver());
        vo.setReceiverPhone(main.getReceiverPhone());
        vo.setReceiverAddress(main.getReceiverAddress());
        vo.setRemark(main.getRemark());
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
     * 商家取消订单后回补库存。
     */
    private void restoreOrderStock(OrderMain main) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, main.getOrderNo()));
        for (OrderItem item : items) {
            productInfoMapper.restoreStockSafely(item.getProductId(), item.getProductNum());
            ProductInfo product = productInfoMapper.selectById(item.getProductId());
            publicDataCacheService.refreshHotProduct(product);
            publicDataCacheService.evictProductCatalog(item.getProductId(), main.getMerchantId());
        }
    }
}
