package com.agricultural.assistplatform.job;

import com.agricultural.assistplatform.entity.AfterSale;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.entity.ReconciliationDetail;
import com.agricultural.assistplatform.mapper.AfterSaleMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.ReconciliationDetailMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动打款定时任务
 * 每天凌晨1点执行，为已完成7天且无售后纠纷的订单自动打款
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoTransferJob {

    private final OrderMainMapper orderMainMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final ReconciliationDetailMapper reconciliationDetailMapper;

    /**
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoTransfer() {
        log.info("开始执行自动打款任务...");

        // 计算7天前的时间
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 查询已完成且超过7天的订单
        LambdaQueryWrapper<OrderMain> orderQuery = new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderStatus, 4) // 已完成状态
                .lt(OrderMain::getUpdateTime, sevenDaysAgo) // 7天前完成的
                .eq(OrderMain::getDeleteFlag, 0);

        List<OrderMain> completedOrders = orderMainMapper.selectList(orderQuery);

        if (completedOrders.isEmpty()) {
            log.info("没有需要自动打款的订单");
            return;
        }

        log.info("发现 {} 个订单需要自动打款", completedOrders.size());

        int successCount = 0;
        for (OrderMain order : completedOrders) {
            try {
                // 检查是否有未解决的售后
                if (hasUnresolvedAfterSale(order.getOrderNo())) {
                    log.info("订单 {} 存在未解决的售后，跳过打款", order.getOrderNo());
                    continue;
                }

                // 检查是否已打款
                if (isAlreadyTransferred(order.getOrderNo())) {
                    log.info("订单 {} 已打款，跳过", order.getOrderNo());
                    continue;
                }

                // 执行打款
                transferToMerchant(order);
                successCount++;
            } catch (Exception e) {
                log.error("订单 {} 自动打款失败: {}", order.getOrderNo(), e.getMessage());
            }
        }

        log.info("自动打款任务执行完成，成功打款 {} 笔", successCount);
    }

    /**
     * 检查订单是否有未解决的售后
     */
    private boolean hasUnresolvedAfterSale(String orderNo) {
        LambdaQueryWrapper<AfterSale> query = new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderNo, orderNo)
                .in(AfterSale::getAfterSaleStatus, 1, 2, 4) // 待处理、协商中、管理员介入
                .eq(AfterSale::getDeleteFlag, 0);
        return afterSaleMapper.selectCount(query) > 0;
    }

    /**
     * 检查订单是否已打款
     */
    private boolean isAlreadyTransferred(String orderNo) {
        LambdaQueryWrapper<ReconciliationDetail> query = new LambdaQueryWrapper<ReconciliationDetail>()
                .eq(ReconciliationDetail::getOrderNo, orderNo)
                .eq(ReconciliationDetail::getTransferStatus, 1) // 已打款
                .eq(ReconciliationDetail::getDeleteFlag, 0);
        return reconciliationDetailMapper.selectCount(query) > 0;
    }

    /**
     * 执行打款到商家
     */
    private void transferToMerchant(OrderMain order) {
        // 计算实际营收（扣除平台服务费，假设5%）
        java.math.BigDecimal serviceFee = order.getTotalAmount()
                .multiply(java.math.BigDecimal.valueOf(0.05));
        java.math.BigDecimal actualIncome = order.getTotalAmount().subtract(serviceFee);

        ReconciliationDetail detail = new ReconciliationDetail();
        detail.setMerchantId(order.getMerchantId());
        detail.setOrderNo(order.getOrderNo());
        detail.setOrderAmount(order.getTotalAmount());
        detail.setActualIncome(actualIncome);
        detail.setServiceFee(serviceFee);
        detail.setPaymentTime(LocalDateTime.now());
        detail.setTransferStatus(1); // 已打款
        detail.setTransferTime(LocalDateTime.now());
        detail.setTransferNo("AUTO_" + System.currentTimeMillis());

        reconciliationDetailMapper.insert(detail);

        log.info("订单 {} 自动打款成功，金额: {}", order.getOrderNo(), actualIncome);
    }
}
