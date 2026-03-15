package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.AuditRecord;
import com.agricultural.assistplatform.mapper.AuditRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuditRecordService {

    private final AuditRecordMapper auditRecordMapper;

    public PageResult<AuditRecord> list(String businessType, Integer auditStatus, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<AuditRecord> q = new LambdaQueryWrapper<AuditRecord>();
        if (businessType != null && !businessType.isBlank()) q.eq(AuditRecord::getBusinessType, businessType);
        if (auditStatus != null) q.eq(AuditRecord::getAuditStatus, auditStatus);
        q.orderByDesc(AuditRecord::getCreateTime);
        Page<AuditRecord> page = auditRecordMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }
}
