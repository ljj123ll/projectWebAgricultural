package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.PaymentRecord;
import com.agricultural.assistplatform.mapper.PaymentRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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

    public void updateRefundStatus(Long id, Integer refundStatus) {
        PaymentRecord pr = new PaymentRecord();
        pr.setId(id);
        pr.setRefundStatus(refundStatus);
        paymentRecordMapper.updateById(pr);
    }
}
