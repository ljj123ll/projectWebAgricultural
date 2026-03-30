package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.PaymentRecord;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.PaymentRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
/**
 * 管理员支付与退款服务。
 * 负责支付记录分页查询，以及退款中记录的最终处理。
 */
public class AdminPaymentService {

    private final PaymentRecordMapper paymentRecordMapper;

    public PageResult<PaymentRecord> list(Integer payStatus, Integer refundStatus, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<PaymentRecord> q = new LambdaQueryWrapper<PaymentRecord>();
        if (payStatus != null) q.eq(PaymentRecord::getPayStatus, payStatus);
        if (refundStatus != null) q.eq(PaymentRecord::getRefundStatus, refundStatus);
        q.orderByDesc(PaymentRecord::getCreateTime);
        Page<PaymentRecord> page = paymentRecordMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    /**
     * 管理员处理退款。
     * 这里只允许把“退款中”流转为“退款成功”或“退款失败”，并同步写入退款金额和时间。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRefundStatus(Long id, Integer refundStatus) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "支付记录不存在");
        }
        if (refundStatus == null || (refundStatus != 2 && refundStatus != 3)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "退款处理结果不合法");
        }
        PaymentRecord record = paymentRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付记录不存在");
        }
        if (!Integer.valueOf(1).equals(record.getPayStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支付成功记录支持退款处理");
        }
        if (Integer.valueOf(refundStatus).equals(record.getRefundStatus())) {
            return;
        }
        if (!Integer.valueOf(1).equals(record.getRefundStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "退款状态已处理，请刷新后重试");
        }

        BigDecimal refundAmount = refundStatus == 2
                ? (record.getPayAmount() != null ? record.getPayAmount() : BigDecimal.ZERO)
                : BigDecimal.ZERO;
        LocalDateTime refundTime = refundStatus == 2 ? LocalDateTime.now() : null;

        int updated = paymentRecordMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PaymentRecord>()
                .eq(PaymentRecord::getId, id)
                .eq(PaymentRecord::getPayStatus, 1)
                .eq(PaymentRecord::getRefundStatus, 1)
                .set(PaymentRecord::getRefundStatus, refundStatus)
                .set(PaymentRecord::getRefundAmount, refundAmount)
                .set(PaymentRecord::getRefundTime, refundTime));
        if (updated < 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "退款状态已变更，请刷新后重试");
        }
    }
}
