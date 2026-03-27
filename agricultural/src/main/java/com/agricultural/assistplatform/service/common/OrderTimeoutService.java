package com.agricultural.assistplatform.service.common;

import com.agricultural.assistplatform.entity.OrderItem;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.mapper.OrderItemMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTimeoutService {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductInfoMapper productInfoMapper;
    private final PublicDataCacheService publicDataCacheService;
    private final UserRealtimeEventService userRealtimeEventService;
    private final MerchantRealtimeEventService merchantRealtimeEventService;
    private final AdminRealtimeEventService adminRealtimeEventService;

    @Transactional(rollbackFor = Exception.class)
    public int cancelTimeoutOrders() {
        List<OrderMain> timeoutOrders = orderMainMapper.selectList(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderStatus, 1)
                .lt(OrderMain::getPayDeadline, LocalDateTime.now())
                .eq(OrderMain::getDeleteFlag, 0));
        if (timeoutOrders.isEmpty()) {
            return 0;
        }

        for (OrderMain order : timeoutOrders) {
            cancelTimeoutOrder(order);
        }
        return timeoutOrders.size();
    }

    private void cancelTimeoutOrder(OrderMain order) {
        order.setOrderStatus(5);
        order.setCancelReason("超时未付款，系统自动取消");
        orderMainMapper.updateById(order);

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderNo, order.getOrderNo()));
        for (OrderItem item : items) {
            productInfoMapper.update(null, new LambdaUpdateWrapper<ProductInfo>()
                    .eq(ProductInfo::getId, item.getProductId())
                    .setSql("stock = stock + " + item.getProductNum())
                    .setSql("sales_volume = GREATEST(sales_volume - " + item.getProductNum() + ", 0)"));
            ProductInfo product = productInfoMapper.selectById(item.getProductId());
            publicDataCacheService.refreshHotProduct(product);
            publicDataCacheService.evictProductCatalog(item.getProductId(), order.getMerchantId());
        }

        if (order.getUserId() != null) {
            userRealtimeEventService.publishRefresh(order.getUserId(), "ORDER_TIMEOUT_CANCELED", order.getOrderNo());
        }
        if (order.getMerchantId() != null) {
            merchantRealtimeEventService.publishRefresh(order.getMerchantId(), "ORDER_TIMEOUT_CANCELED", order.getOrderNo());
        }
        adminRealtimeEventService.publishRefreshToAll("ORDER_TIMEOUT_CANCELED", order.getOrderNo());
        log.info("订单 {} 超时已取消并完成库存、销量与缓存回滚", order.getOrderNo());
    }
}
