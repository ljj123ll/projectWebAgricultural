package com.agricultural.assistplatform.job;

import com.agricultural.assistplatform.service.common.MerchantWithdrawTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantWithdrawTransferJob {

    private final MerchantWithdrawTransferService merchantWithdrawTransferService;

    /**
     * 每分钟处理一次提现打款任务（包含失败重试）
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void processWithdrawTransfer() {
        try {
            merchantWithdrawTransferService.processAutoBatch(100);
        } catch (Exception ex) {
            log.error("提现自动打款任务执行失败", ex);
        }
    }
}

