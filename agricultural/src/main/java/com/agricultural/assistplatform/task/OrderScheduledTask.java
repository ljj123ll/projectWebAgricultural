package com.agricultural.assistplatform.task;

import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.entity.OrderItem;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.mapper.OrderItemMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderScheduledTask {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductInfoMapper productInfoMapper;

    /**
     * 1. 自动取消超时未支付订单 (每分钟执行一次)
     * 规则: 15分钟内未付款自动取消，取消后库存自动回滚
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoCancelUnpaidOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<OrderMain> timeoutOrders = orderMainMapper.selectList(
                new LambdaQueryWrapper<OrderMain>()
                        .eq(OrderMain::getOrderStatus, 1) // 待付款
                        .lt(OrderMain::getPayDeadline, now)
                        .eq(OrderMain::getDeleteFlag, 0)
        );

        if (!timeoutOrders.isEmpty()) {
            log.info("发现 {} 个超时未支付订单，准备取消并回滚库存", timeoutOrders.size());
            for (OrderMain order : timeoutOrders) {
                // 更新订单状态为已取消
                order.setOrderStatus(5);
                order.setCancelReason("超时未付款系统自动取消");
                orderMainMapper.updateById(order);

                // 回滚库存
                List<OrderItem> items = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, order.getOrderNo())
                );
                for (OrderItem item : items) {
                    productInfoMapper.update(null, new LambdaUpdateWrapper<ProductInfo>()
                            .eq(ProductInfo::getId, item.getProductId())
                            .setSql("stock = stock + " + item.getProductNum())
                            // 可选：销量也回滚
                            .setSql("sales_volume = sales_volume - " + item.getProductNum())
                    );
                }
                log.info("订单 {} 超时已取消，库存已回滚", order.getOrderNo());
            }
        }
    }


}
