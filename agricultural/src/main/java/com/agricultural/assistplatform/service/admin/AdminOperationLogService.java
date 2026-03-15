package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.SysOperationLog;
import com.agricultural.assistplatform.mapper.SysOperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminOperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    public PageResult<SysOperationLog> list(String operationType, String businessType, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<SysOperationLog> q = new LambdaQueryWrapper<SysOperationLog>();
        if (operationType != null && !operationType.isBlank()) q.eq(SysOperationLog::getOperationType, operationType);
        if (businessType != null && !businessType.isBlank()) q.eq(SysOperationLog::getBusinessType, businessType);
        q.orderByDesc(SysOperationLog::getCreateTime);
        Page<SysOperationLog> page = sysOperationLogMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }
}
