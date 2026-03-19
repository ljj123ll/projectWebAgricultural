package com.agricultural.assistplatform.job;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消定时任务
 * 每5分钟执行一次，检查并取消超时未付款的订单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductInfoMapper productInfoMapper;

    /**
     * 每5分钟执行一次
     * cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrders() {
        log.info("开始执行订单超时自动取消任务...");
        
        // 查询待付款且已超时的订单
        LambdaQueryWrapper<OrderMain> query = new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderStatus, 1) // 待付款状态
                .lt(OrderMain::getPayDeadline, LocalDateTime.now()) // 支付截止时间已过
                .eq(OrderMain::getDeleteFlag, 0);
        
        List<OrderMain> timeoutOrders = orderMainMapper.selectList(query);
        
        if (timeoutOrders.isEmpty()) {
            log.info("没有超时的订单需要处理");
            return;
        }
        
        log.info("发现 {} 个超时订单需要取消", timeoutOrders.size());
        
        for (OrderMain order : timeoutOrders) {
            try {
                cancelOrder(order);
            } catch (Exception e) {
                log.error("取消订单 {} 失败: {}", order.getOrderNo(), e.getMessage());
            }
        }
        
        log.info("订单超时自动取消任务执行完成");
    }
    
    private void cancelOrder(OrderMain order) {
        // 1. 更新订单状态为已取消
        order.setOrderStatus(5); // 已取消
        order.setCancelReason("超时未付款，系统自动取消");
        orderMainMapper.updateById(order);
        
        // 2. 回滚库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderNo, order.getOrderNo())
        );
        
        for (OrderItem item : items) {
            productInfoMapper.update(null,
                    new LambdaUpdateWrapper<ProductInfo>()
                            .eq(ProductInfo::getId, item.getProductId())
                            .setSql("stock = stock + " + item.getProductNum())
            );
            log.info("订单 {} 商品 {} 库存回滚 {}", 
                    order.getOrderNo(), item.getProductId(), item.getProductNum());
        }
        
        log.info("订单 {} 已自动取消", order.getOrderNo());
    }
}
