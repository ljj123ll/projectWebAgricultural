package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.MerchantWithdrawStatus;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.dto.merchant.MerchantWithdrawApplyDTO;
import com.agricultural.assistplatform.entity.MerchantAccount;
import com.agricultural.assistplatform.entity.MerchantWithdraw;
import com.agricultural.assistplatform.entity.ReconciliationDetail;
import com.agricultural.assistplatform.entity.SubsidyDetail;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.MerchantAccountMapper;
import com.agricultural.assistplatform.mapper.MerchantWithdrawMapper;
import com.agricultural.assistplatform.mapper.ReconciliationDetailMapper;
import com.agricultural.assistplatform.mapper.SubsidyDetailMapper;
import com.agricultural.assistplatform.service.common.MerchantRealtimeEventService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MerchantWithdrawService {

    private final MerchantWithdrawMapper merchantWithdrawMapper;
    private final MerchantAccountMapper merchantAccountMapper;
    private final ReconciliationDetailMapper reconciliationDetailMapper;
    private final SubsidyDetailMapper subsidyDetailMapper;
    private final MerchantRealtimeEventService merchantRealtimeEventService;

    @Transactional(rollbackFor = Exception.class)
    public String apply(MerchantWithdrawApplyDTO body) {
        Long merchantId = requireMerchantId();
        if (body == null || body.getAmount() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提现金额不能为空");
        }

        BigDecimal amount = body.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ONE) < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "单次提现金额不得低于1元");
        }
        if (amount.compareTo(BigDecimal.valueOf(50000)) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "单次提现金额不得超过50000元");
        }

        MerchantAccount account = selectWithdrawAccount(merchantId, body.getAccountId());
        BigDecimal available = getAvailableAmount(merchantId);
        if (amount.compareTo(available) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "可提现余额不足，当前可提现：" + available);
        }

        BigDecimal feeAmount = BigDecimal.ZERO;
        BigDecimal actualAmount = amount.subtract(feeAmount);

        MerchantWithdraw withdraw = new MerchantWithdraw();
        withdraw.setWithdrawNo(generateWithdrawNo());
        withdraw.setMerchantId(merchantId);
        withdraw.setAccountId(account.getId());
        withdraw.setAccountType(account.getAccountType());
        withdraw.setAccountNo(account.getAccountNo());
        withdraw.setAccountName(account.getAccountName());
        withdraw.setApplyAmount(amount);
        withdraw.setFeeAmount(feeAmount);
        withdraw.setActualAmount(actualAmount);
        withdraw.setStatus(MerchantWithdrawStatus.PENDING_AUDIT);
        withdraw.setRetryCount(0);
        merchantWithdrawMapper.insert(withdraw);

        merchantRealtimeEventService.publishRefresh(merchantId, "WITHDRAW_APPLIED", withdraw.getWithdrawNo());
        return withdraw.getWithdrawNo();
    }

    public PageResult<MerchantWithdraw> list(
            Integer status,
            String keyword,
            String startDate,
            String endDate,
            Integer pageNum,
            Integer pageSize) {
        Long merchantId = requireMerchantId();
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        LocalDate start = parseDate(startDate, "开始日期");
        LocalDate end = parseDate(endDate, "结束日期");
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }

        LambdaQueryWrapper<MerchantWithdraw> query = new LambdaQueryWrapper<MerchantWithdraw>()
                .eq(MerchantWithdraw::getMerchantId, merchantId);
        if (status != null) {
            query.eq(MerchantWithdraw::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            query.like(MerchantWithdraw::getWithdrawNo, keyword.trim());
        }
        if (start != null) {
            query.ge(MerchantWithdraw::getCreateTime, start.atStartOfDay());
        }
        if (end != null) {
            query.lt(MerchantWithdraw::getCreateTime, end.plusDays(1).atStartOfDay());
        }
        query.orderByDesc(MerchantWithdraw::getCreateTime);

        Page<MerchantWithdraw> page = merchantWithdrawMapper.selectPage(new Page<>(pageNum, pageSize), query);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Long merchantId = requireMerchantId();
        MerchantWithdraw withdraw = merchantWithdrawMapper.selectOne(new LambdaQueryWrapper<MerchantWithdraw>()
                .eq(MerchantWithdraw::getId, id)
                .eq(MerchantWithdraw::getMerchantId, merchantId)
                .last("LIMIT 1"));
        if (withdraw == null) throw new BusinessException(ResultCode.NOT_FOUND, "提现单不存在");
        if (withdraw.getStatus() == null || withdraw.getStatus() != MerchantWithdrawStatus.PENDING_AUDIT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待审核提现单可取消");
        }
        withdraw.setStatus(MerchantWithdrawStatus.CANCELED);
        withdraw.setAuditRemark("商家取消提现");
        withdraw.setAuditTime(LocalDateTime.now());
        merchantWithdrawMapper.updateById(withdraw);
        merchantRealtimeEventService.publishRefresh(merchantId, "WITHDRAW_CANCELED", withdraw.getWithdrawNo());
    }

    public Map<String, BigDecimal> available() {
        Long merchantId = requireMerchantId();
        BigDecimal available = getAvailableAmount(merchantId);
        BigDecimal frozen = getFrozenAmount(merchantId);
        BigDecimal success = getSuccessWithdrawAmount(merchantId);
        return Map.of(
                "availableAmount", available,
                "frozenAmount", frozen,
                "withdrawSuccessAmount", success
        );
    }

    public BigDecimal getAvailableAmount(Long merchantId) {
        BigDecimal incomeTotal = getIncomeTotal(merchantId);
        BigDecimal subsidyTotal = getSubsidyGrantedTotal(merchantId);
        BigDecimal consumed = getConsumedWithdrawAmount(merchantId);
        BigDecimal available = incomeTotal.add(subsidyTotal).subtract(consumed);
        if (available.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        return available.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getFrozenAmount(Long merchantId) {
        List<MerchantWithdraw> list = merchantWithdrawMapper.selectList(new LambdaQueryWrapper<MerchantWithdraw>()
                .eq(MerchantWithdraw::getMerchantId, merchantId)
                .in(MerchantWithdraw::getStatus,
                        MerchantWithdrawStatus.PENDING_AUDIT,
                        MerchantWithdrawStatus.APPROVED_WAIT_TRANSFER,
                        MerchantWithdrawStatus.TRANSFER_FAILED_RETRY,
                        MerchantWithdrawStatus.TRANSFER_FAILED_MANUAL));
        BigDecimal total = BigDecimal.ZERO;
        for (MerchantWithdraw item : list) {
            total = total.add(nvl(item.getActualAmount()));
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getSuccessWithdrawAmount(Long merchantId) {
        List<MerchantWithdraw> list = merchantWithdrawMapper.selectList(new LambdaQueryWrapper<MerchantWithdraw>()
                .eq(MerchantWithdraw::getMerchantId, merchantId)
                .eq(MerchantWithdraw::getStatus, MerchantWithdrawStatus.TRANSFER_SUCCESS));
        BigDecimal total = BigDecimal.ZERO;
        for (MerchantWithdraw item : list) {
            total = total.add(nvl(item.getActualAmount()));
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal getIncomeTotal(Long merchantId) {
        List<ReconciliationDetail> list = reconciliationDetailMapper.selectList(new LambdaQueryWrapper<ReconciliationDetail>()
                .eq(ReconciliationDetail::getMerchantId, merchantId)
                .eq(ReconciliationDetail::getTransferStatus, 1));
        BigDecimal total = BigDecimal.ZERO;
        for (ReconciliationDetail item : list) {
            total = total.add(nvl(item.getActualIncome()));
        }
        return total;
    }

    private BigDecimal getSubsidyGrantedTotal(Long merchantId) {
        List<SubsidyDetail> list = subsidyDetailMapper.selectList(new LambdaQueryWrapper<SubsidyDetail>()
                .eq(SubsidyDetail::getMerchantId, merchantId)
                .eq(SubsidyDetail::getGrantStatus, 1));
        BigDecimal total = BigDecimal.ZERO;
        for (SubsidyDetail item : list) {
            total = total.add(nvl(item.getSubsidyAmount()));
        }
        return total;
    }

    private BigDecimal getConsumedWithdrawAmount(Long merchantId) {
        List<MerchantWithdraw> list = merchantWithdrawMapper.selectList(new LambdaQueryWrapper<MerchantWithdraw>()
                .eq(MerchantWithdraw::getMerchantId, merchantId)
                .in(MerchantWithdraw::getStatus,
                        MerchantWithdrawStatus.PENDING_AUDIT,
                        MerchantWithdrawStatus.APPROVED_WAIT_TRANSFER,
                        MerchantWithdrawStatus.TRANSFER_SUCCESS,
                        MerchantWithdrawStatus.TRANSFER_FAILED_RETRY,
                        MerchantWithdrawStatus.TRANSFER_FAILED_MANUAL));
        BigDecimal total = BigDecimal.ZERO;
        for (MerchantWithdraw item : list) {
            total = total.add(nvl(item.getActualAmount()));
        }
        return total;
    }

    private MerchantAccount selectWithdrawAccount(Long merchantId, Long accountId) {
        LambdaQueryWrapper<MerchantAccount> query = new LambdaQueryWrapper<MerchantAccount>()
                .eq(MerchantAccount::getMerchantId, merchantId)
                .eq(MerchantAccount::getVerifyStatus, 2)
                .eq(MerchantAccount::getAuditStatus, 1);
        if (accountId != null) {
            query.eq(MerchantAccount::getId, accountId).last("LIMIT 1");
        } else {
            query.orderByDesc(MerchantAccount::getUpdateTime).last("LIMIT 1");
        }
        MerchantAccount account = merchantAccountMapper.selectOne(query);
        if (account == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未找到可用收款账户，请先完成1分钱验证并通过管理员审核");
        }
        return account;
    }

    private Long requireMerchantId() {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        return merchantId;
    }

    private LocalDate parseDate(String raw, String label) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "格式错误，应为yyyy-MM-dd");
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String generateWithdrawNo() {
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "WD" + System.currentTimeMillis() + rand;
    }
}

