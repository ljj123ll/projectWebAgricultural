package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.MerchantWithdrawStatus;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.ReconciliationDetail;
import com.agricultural.assistplatform.entity.MerchantAccount;
import com.agricultural.assistplatform.entity.MerchantWithdraw;
import com.agricultural.assistplatform.entity.SubsidyDetail;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.MerchantAccountMapper;
import com.agricultural.assistplatform.mapper.MerchantWithdrawMapper;
import com.agricultural.assistplatform.mapper.ReconciliationDetailMapper;
import com.agricultural.assistplatform.mapper.SubsidyDetailMapper;
import com.agricultural.assistplatform.vo.merchant.MerchantAccountOverviewVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantAccountService {

    private final MerchantAccountMapper merchantAccountMapper;
    private final ReconciliationDetailMapper reconciliationDetailMapper;
    private final SubsidyDetailMapper subsidyDetailMapper;
    private final MerchantWithdrawMapper merchantWithdrawMapper;

    public List<MerchantAccount> list() {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        return merchantAccountMapper.selectList(new LambdaQueryWrapper<MerchantAccount>()
                .eq(MerchantAccount::getMerchantId, merchantId)
                .orderByDesc(MerchantAccount::getUpdateTime));
    }

    public MerchantAccountOverviewVO overview() {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        LocalDate monthStart = today.withDayOfMonth(1);

        List<ReconciliationDetail> reconciliationList = reconciliationDetailMapper.selectList(
                new LambdaQueryWrapper<ReconciliationDetail>()
                        .eq(ReconciliationDetail::getMerchantId, merchantId)
                        .orderByDesc(ReconciliationDetail::getCreateTime));

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalServiceFee = BigDecimal.ZERO;
        BigDecimal todayIncome = BigDecimal.ZERO;
        BigDecimal weekIncome = BigDecimal.ZERO;
        BigDecimal monthIncome = BigDecimal.ZERO;
        int pendingTransferCount = 0;
        int failedTransferCount = 0;
        int manualFallbackCount = 0;

        for (ReconciliationDetail detail : reconciliationList) {
            Integer transferStatus = detail.getTransferStatus();
            if (transferStatus != null) {
                if (transferStatus == 0) pendingTransferCount++;
                if (transferStatus == 2) failedTransferCount++;
                if (transferStatus == 3) manualFallbackCount++;
            }

            if (transferStatus == null || transferStatus != 1) continue;
            BigDecimal income = nvl(detail.getActualIncome());
            if (income.compareTo(BigDecimal.ZERO) < 0) continue;

            totalIncome = totalIncome.add(income);
            totalServiceFee = totalServiceFee.add(nvl(detail.getServiceFee()));

            LocalDate bizDate = getBizDate(detail.getTransferTime(), detail.getCreateTime());
            if (bizDate == null) continue;
            if (bizDate.isEqual(today)) todayIncome = todayIncome.add(income);
            if (!bizDate.isBefore(weekStart)) weekIncome = weekIncome.add(income);
            if (!bizDate.isBefore(monthStart)) monthIncome = monthIncome.add(income);
        }

        List<SubsidyDetail> subsidyList = subsidyDetailMapper.selectList(new LambdaQueryWrapper<SubsidyDetail>()
                .eq(SubsidyDetail::getMerchantId, merchantId)
                .orderByDesc(SubsidyDetail::getCreateTime));

        BigDecimal subsidyTotal = BigDecimal.ZERO;
        BigDecimal subsidyMonth = BigDecimal.ZERO;
        int subsidyPendingCount = 0;
        int subsidyRejectedCount = 0;

        for (SubsidyDetail subsidy : subsidyList) {
            Integer auditStatus = subsidy.getAuditStatus();
            Integer grantStatus = subsidy.getGrantStatus();

            if (auditStatus != null && auditStatus == 2) subsidyRejectedCount++;
            if ((auditStatus != null && auditStatus == 0) || (auditStatus != null && auditStatus == 1 && (grantStatus == null || grantStatus == 0))) {
                subsidyPendingCount++;
            }

            if (grantStatus == null || grantStatus != 1) continue;
            BigDecimal subsidyAmount = nvl(subsidy.getSubsidyAmount());
            subsidyTotal = subsidyTotal.add(subsidyAmount);

            LocalDate bizDate = getBizDate(subsidy.getGrantTime(), subsidy.getCreateTime());
            if (bizDate != null && !bizDate.isBefore(monthStart)) {
                subsidyMonth = subsidyMonth.add(subsidyAmount);
            }
        }

        MerchantAccount approvedAccount = merchantAccountMapper.selectOne(new LambdaQueryWrapper<MerchantAccount>()
                .eq(MerchantAccount::getMerchantId, merchantId)
                .eq(MerchantAccount::getVerifyStatus, 2)
                .eq(MerchantAccount::getAuditStatus, 1)
                .orderByDesc(MerchantAccount::getUpdateTime)
                .last("LIMIT 1"));

        MerchantAccountOverviewVO vo = new MerchantAccountOverviewVO();
        BigDecimal withdrawSuccessAmount = sumWithdrawAmount(merchantId,
                MerchantWithdrawStatus.TRANSFER_SUCCESS);
        BigDecimal withdrawFrozenAmount = sumWithdrawAmount(merchantId,
                MerchantWithdrawStatus.PENDING_AUDIT,
                MerchantWithdrawStatus.APPROVED_WAIT_TRANSFER,
                MerchantWithdrawStatus.TRANSFER_FAILED_RETRY,
                MerchantWithdrawStatus.TRANSFER_FAILED_MANUAL);
        BigDecimal withdrawAvailableAmount = totalIncome.add(subsidyTotal)
                .subtract(withdrawSuccessAmount)
                .subtract(withdrawFrozenAmount);
        if (withdrawAvailableAmount.compareTo(BigDecimal.ZERO) < 0) {
            withdrawAvailableAmount = BigDecimal.ZERO;
        }

        vo.setBalance(withdrawAvailableAmount);
        vo.setTotalIncome(totalIncome);
        vo.setTotalServiceFee(totalServiceFee);
        vo.setTodayIncome(todayIncome);
        vo.setWeekIncome(weekIncome);
        vo.setMonthIncome(monthIncome);
        vo.setPendingTransferCount(pendingTransferCount);
        vo.setFailedTransferCount(failedTransferCount);
        vo.setManualFallbackCount(manualFallbackCount);
        vo.setSubsidyTotal(subsidyTotal);
        vo.setSubsidyMonth(subsidyMonth);
        vo.setSubsidyOrderCount(subsidyList.size());
        vo.setSubsidyPendingCount(subsidyPendingCount);
        vo.setSubsidyRejectedCount(subsidyRejectedCount);
        vo.setWithdrawAvailableAmount(withdrawAvailableAmount);
        vo.setWithdrawFrozenAmount(withdrawFrozenAmount);
        vo.setWithdrawSuccessAmount(withdrawSuccessAmount);
        vo.setHasPassedReceivingAccount(approvedAccount != null);
        if (approvedAccount != null) {
            vo.setApprovedAccountType(approvedAccount.getAccountType());
            vo.setApprovedAccountName(approvedAccount.getAccountName());
            vo.setApprovedAccountNoMask(maskAccountNo(approvedAccount.getAccountNo()));
        }
        return vo;
    }

    public void save(MerchantAccount body) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (body == null) throw new BusinessException(ResultCode.BAD_REQUEST, "账户信息不能为空");
        if (body.getAccountType() == null) throw new BusinessException(ResultCode.BAD_REQUEST, "账户类型不能为空");
        String accountNo = body.getAccountNo() == null ? "" : body.getAccountNo().trim();
        String accountName = body.getAccountName() == null ? "" : body.getAccountName().trim();
        if (accountNo.isBlank() || accountName.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写完整的账户信息");
        }

        MerchantAccount exist = merchantAccountMapper.selectOne(new LambdaQueryWrapper<MerchantAccount>()
                .eq(MerchantAccount::getMerchantId, merchantId)
                .eq(MerchantAccount::getAccountType, body.getAccountType()));

        if (exist == null) {
            MerchantAccount a = new MerchantAccount();
            a.setMerchantId(merchantId);
            a.setAccountType(body.getAccountType());
            a.setAccountNo(accountNo);
            a.setAccountName(accountName);
            a.setVerifyStatus(0); // 0-未验证 1-验证中 2-已验证
            a.setAuditStatus(0); // 0-待审核 1-通过 2-驳回
            merchantAccountMapper.insert(a);
        } else {
            boolean changed = !accountNo.equals(exist.getAccountNo()) || !accountName.equals(exist.getAccountName());
            exist.setAccountNo(accountNo);
            exist.setAccountName(accountName);
            if (changed) {
                // 账户信息变更后，必须重新进行打款验证并进入审核
                exist.setVerifyStatus(0);
                exist.setAuditStatus(0);
                exist.setVerifyAmount(null);
                exist.setVerifyExpireTime(null);
                exist.setVerifiedTime(null);
                exist.setAuditSubmitTime(null);
                exist.setRejectReason(null);
            }
            merchantAccountMapper.updateById(exist);
        }
    }

    public BigDecimal initVerify(Long id) {
        MerchantAccount account = getAccount(id);
        BigDecimal amount = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(11, 100))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        account.setVerifyAmount(amount);
        account.setVerifyStatus(1);
        account.setVerifyExpireTime(LocalDateTime.now().plusMinutes(30));
        account.setVerifiedTime(null);
        account.setAuditStatus(0);
        account.setAuditSubmitTime(null);
        account.setRejectReason(null);
        merchantAccountMapper.updateById(account);
        return amount;
    }

    public void confirmVerify(Long id, BigDecimal amount) {
        MerchantAccount account = getAccount(id);
        if (account.getVerifyAmount() == null || account.getVerifyExpireTime() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请先发起1分钱账户验证");
        }
        if (account.getVerifyExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证金额已过期，请重新发起验证");
        }
        if (amount == null) throw new BusinessException(ResultCode.BAD_REQUEST, "请输入到账验证金额");

        BigDecimal expected = account.getVerifyAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal actual = amount.setScale(2, RoundingMode.HALF_UP);
        if (expected.compareTo(actual) != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证金额不匹配，请核对后重试");
        }
        account.setVerifyStatus(2);
        account.setVerifiedTime(LocalDateTime.now());
        account.setVerifyExpireTime(null);
        merchantAccountMapper.updateById(account);
    }

    public void submitAudit(Long id) {
        MerchantAccount account = getAccount(id);
        if (account.getVerifyStatus() == null || account.getVerifyStatus() != 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请先完成1分钱验证后再提交审核");
        }
        account.setAuditStatus(0);
        account.setAuditSubmitTime(LocalDateTime.now());
        account.setRejectReason(null);
        merchantAccountMapper.updateById(account);
    }

    private MerchantAccount getAccount(Long id) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        MerchantAccount account = merchantAccountMapper.selectOne(new LambdaQueryWrapper<MerchantAccount>()
                .eq(MerchantAccount::getId, id)
                .eq(MerchantAccount::getMerchantId, merchantId)
                .last("LIMIT 1"));
        if (account == null) throw new BusinessException(ResultCode.NOT_FOUND, "收款账户不存在");
        return account;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDate getBizDate(LocalDateTime primary, LocalDateTime fallback) {
        if (primary != null) return primary.toLocalDate();
        if (fallback != null) return fallback.toLocalDate();
        return null;
    }

    private String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) return "";
        String no = accountNo.trim();
        if (no.length() <= 8) return no;
        return no.substring(0, 4) + " **** **** " + no.substring(no.length() - 4);
    }

    private BigDecimal sumWithdrawAmount(Long merchantId, Integer... statuses) {
        List<MerchantWithdraw> list = merchantWithdrawMapper.selectList(new LambdaQueryWrapper<MerchantWithdraw>()
                .eq(MerchantWithdraw::getMerchantId, merchantId)
                .in(MerchantWithdraw::getStatus, (Object[]) statuses));
        BigDecimal total = BigDecimal.ZERO;
        for (MerchantWithdraw item : list) {
            total = total.add(nvl(item.getActualAmount()));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
