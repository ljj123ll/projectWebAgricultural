package com.agricultural.assistplatform.job;

import com.agricultural.assistplatform.service.common.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时自动取消定时任务
 * 每分钟执行一次，统一由 OrderTimeoutService 处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderTimeoutService orderTimeoutService;

    /**
     * 每分钟执行一次
     * cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 * * * * ?")
    public void cancelTimeoutOrders() {
        int count = orderTimeoutService.cancelTimeoutOrders();
        if (count == 0) {
            log.info("没有超时的订单需要处理");
            return;
        }
        log.info("订单超时自动取消任务执行完成，本轮处理 {} 笔订单", count);
    }
}
