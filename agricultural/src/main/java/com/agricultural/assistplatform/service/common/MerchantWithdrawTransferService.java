package com.agricultural.assistplatform.service.common;

import com.agricultural.assistplatform.common.MerchantWithdrawStatus;
import com.agricultural.assistplatform.entity.MerchantAccount;
import com.agricultural.assistplatform.entity.MerchantWithdraw;
import com.agricultural.assistplatform.mapper.MerchantAccountMapper;
import com.agricultural.assistplatform.mapper.MerchantWithdrawMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MerchantWithdrawTransferService {

    private final MerchantWithdrawMapper merchantWithdrawMapper;
    private final MerchantAccountMapper merchantAccountMapper;
    private final MerchantRealtimeEventService merchantRealtimeEventService;

    @Transactional(rollbackFor = Exception.class)
    public void processAutoBatch(int batchSize) {
        int size = Math.max(batchSize, 1);
        List<MerchantWithdraw> list = merchantWithdrawMapper.selectList(new LambdaQueryWrapper<MerchantWithdraw>()
                .in(MerchantWithdraw::getStatus,
                        MerchantWithdrawStatus.APPROVED_WAIT_TRANSFER,
                        MerchantWithdrawStatus.TRANSFER_FAILED_RETRY)
                .orderByAsc(MerchantWithdraw::getUpdateTime)
                .last("LIMIT " + size));
        for (MerchantWithdraw withdraw : list) {
            transferInternal(withdraw, false);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void manualTransfer(Long withdrawId) {
        MerchantWithdraw withdraw = merchantWithdrawMapper.selectById(withdrawId);
        if (withdraw == null) return;
        transferInternal(withdraw, true);
    }

    private void transferInternal(MerchantWithdraw withdraw, boolean manual) {
        if (withdraw.getStatus() == null) return;
        if (!manual && withdraw.getStatus() != MerchantWithdrawStatus.APPROVED_WAIT_TRANSFER
                && withdraw.getStatus() != MerchantWithdrawStatus.TRANSFER_FAILED_RETRY) {
            return;
        }
        if (manual && withdraw.getStatus() != MerchantWithdrawStatus.APPROVED_WAIT_TRANSFER
                && withdraw.getStatus() != MerchantWithdrawStatus.TRANSFER_FAILED_RETRY
                && withdraw.getStatus() != MerchantWithdrawStatus.TRANSFER_FAILED_MANUAL) {
            return;
        }

        String failReason = validateAccount(withdraw);
        if (failReason != null) {
            handleFail(withdraw, failReason, manual);
            return;
        }

        String simulationFail = simulateTransferFailReason(withdraw);
        if (simulationFail != null) {
            handleFail(withdraw, simulationFail, manual);
            return;
        }

        withdraw.setStatus(MerchantWithdrawStatus.TRANSFER_SUCCESS);
        withdraw.setTransferTime(LocalDateTime.now());
        withdraw.setTransferNo((manual ? "MANUAL_" : "AUTO_") + System.currentTimeMillis() + withdraw.getId());
        withdraw.setFailReason(null);
        merchantWithdrawMapper.updateById(withdraw);
        merchantRealtimeEventService.publishRefresh(withdraw.getMerchantId(), "WITHDRAW_TRANSFER_SUCCESS", withdraw.getWithdrawNo());
    }

    private void handleFail(MerchantWithdraw withdraw, String reason, boolean manual) {
        int retryCount = withdraw.getRetryCount() == null ? 0 : withdraw.getRetryCount();
        retryCount++;
        withdraw.setRetryCount(retryCount);
        withdraw.setFailReason(reason);
        withdraw.setTransferNo(null);
        withdraw.setTransferTime(null);
        if (manual) {
            withdraw.setStatus(MerchantWithdrawStatus.TRANSFER_FAILED_MANUAL);
        } else {
            withdraw.setStatus(retryCount >= 3
                    ? MerchantWithdrawStatus.TRANSFER_FAILED_MANUAL
                    : MerchantWithdrawStatus.TRANSFER_FAILED_RETRY);
        }
        merchantWithdrawMapper.updateById(withdraw);
        merchantRealtimeEventService.publishRefresh(withdraw.getMerchantId(), "WITHDRAW_TRANSFER_FAIL", withdraw.getWithdrawNo());
    }

    private String validateAccount(MerchantWithdraw withdraw) {
        MerchantAccount account = merchantAccountMapper.selectOne(new LambdaQueryWrapper<MerchantAccount>()
                .eq(MerchantAccount::getId, withdraw.getAccountId())
                .eq(MerchantAccount::getMerchantId, withdraw.getMerchantId())
                .eq(MerchantAccount::getVerifyStatus, 2)
                .eq(MerchantAccount::getAuditStatus, 1)
                .last("LIMIT 1"));
        if (account == null) {
            return "收款账户未通过验证或审核，无法打款";
        }
        return null;
    }

    private String simulateTransferFailReason(MerchantWithdraw withdraw) {
        String accountNo = withdraw.getAccountNo();
        if (accountNo == null) return "收款账户异常";
        String marker = accountNo.toUpperCase(Locale.ROOT);
        int retryCount = withdraw.getRetryCount() == null ? 0 : withdraw.getRetryCount();
        if (marker.contains("FAIL")) {
            return "打款通道异常，请稍后重试";
        }
        if (marker.contains("RETRY") && retryCount < 2) {
            return "银行返回处理中，系统将自动重试";
        }
        return null;
    }
}

