package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.MerchantWithdrawStatus;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.MerchantWithdraw;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.MerchantWithdrawMapper;
import com.agricultural.assistplatform.service.common.MerchantRealtimeEventService;
import com.agricultural.assistplatform.service.common.MerchantWithdrawTransferService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminWithdrawService {

    private final MerchantWithdrawMapper merchantWithdrawMapper;
    private final MerchantRealtimeEventService merchantRealtimeEventService;
    private final MerchantWithdrawTransferService merchantWithdrawTransferService;
    private final AdminAuditTrailService adminAuditTrailService;

    public PageResult<MerchantWithdraw> list(
            Long merchantId,
            Integer status,
            String keyword,
            String startDate,
            String endDate,
            Integer pageNum,
            Integer pageSize
    ) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LocalDate start = parseDate(startDate, "开始日期");
        LocalDate end = parseDate(endDate, "结束日期");
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }

        LambdaQueryWrapper<MerchantWithdraw> query = new LambdaQueryWrapper<>();
        if (merchantId != null) query.eq(MerchantWithdraw::getMerchantId, merchantId);
        if (status != null) query.eq(MerchantWithdraw::getStatus, status);
        if (StringUtils.hasText(keyword)) query.like(MerchantWithdraw::getWithdrawNo, keyword.trim());
        if (start != null) query.ge(MerchantWithdraw::getCreateTime, start.atStartOfDay());
        if (end != null) query.lt(MerchantWithdraw::getCreateTime, end.plusDays(1).atStartOfDay());
        query.orderByDesc(MerchantWithdraw::getCreateTime);
        Page<MerchantWithdraw> page = merchantWithdrawMapper.selectPage(new Page<>(pageNum, pageSize), query);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, boolean approve, String remark, Long adminId) {
        MerchantWithdraw withdraw = merchantWithdrawMapper.selectById(id);
        if (withdraw == null) throw new BusinessException(ResultCode.NOT_FOUND, "提现单不存在");
        if (withdraw.getStatus() == null || withdraw.getStatus() != MerchantWithdrawStatus.PENDING_AUDIT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待审核提现单可审核");
        }

        withdraw.setAuditAdminId(adminId);
        withdraw.setAuditTime(LocalDateTime.now());
        if (approve) {
            withdraw.setStatus(MerchantWithdrawStatus.APPROVED_WAIT_TRANSFER);
            withdraw.setAuditRemark(StringUtils.hasText(remark) ? remark.trim() : "审核通过");
            withdraw.setFailReason(null);
        } else {
            String reason = StringUtils.hasText(remark) ? remark.trim() : "提现申请审核未通过";
            withdraw.setStatus(MerchantWithdrawStatus.REJECTED);
            withdraw.setAuditRemark(reason);
            withdraw.setFailReason(reason);
        }
        merchantWithdrawMapper.updateById(withdraw);
        adminAuditTrailService.record(
                "withdraw",
                id,
                approve,
                approve ? withdraw.getAuditRemark() : withdraw.getFailReason()
        );
        merchantRealtimeEventService.publishRefresh(withdraw.getMerchantId(), "WITHDRAW_AUDITED", withdraw.getWithdrawNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void manualTransfer(Long id) {
        MerchantWithdraw withdraw = merchantWithdrawMapper.selectById(id);
        if (withdraw == null) throw new BusinessException(ResultCode.NOT_FOUND, "提现单不存在");
        if (withdraw.getStatus() == null || (
                withdraw.getStatus() != MerchantWithdrawStatus.APPROVED_WAIT_TRANSFER
                        && withdraw.getStatus() != MerchantWithdrawStatus.TRANSFER_FAILED_RETRY
                        && withdraw.getStatus() != MerchantWithdrawStatus.TRANSFER_FAILED_MANUAL
        )) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前状态不支持人工打款");
        }
        merchantWithdrawTransferService.manualTransfer(id);
    }

    private LocalDate parseDate(String raw, String label) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "格式错误，应为yyyy-MM-dd");
        }
    }
}
