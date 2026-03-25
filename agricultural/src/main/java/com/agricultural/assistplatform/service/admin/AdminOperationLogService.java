package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.SysOperationLog;
import com.agricultural.assistplatform.mapper.SysOperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    public PageResult<SysOperationLog> list(String operationType, String businessType, String startTime, String endTime,
                                            Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<SysOperationLog> q = buildQuery(operationType, businessType, startTime, endTime);
        q.orderByDesc(SysOperationLog::getCreateTime);
        Page<SysOperationLog> page = sysOperationLogMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public List<SysOperationLog> listForExport(String operationType, String businessType, String startTime, String endTime) {
        LambdaQueryWrapper<SysOperationLog> q = buildQuery(operationType, businessType, startTime, endTime);
        q.orderByDesc(SysOperationLog::getCreateTime);
        return sysOperationLogMapper.selectList(q);
    }

    private LambdaQueryWrapper<SysOperationLog> buildQuery(String operationType, String businessType, String startTime, String endTime) {
        LambdaQueryWrapper<SysOperationLog> q = new LambdaQueryWrapper<>();
        if (operationType != null && !operationType.isBlank()) q.eq(SysOperationLog::getOperationType, operationType.trim());
        if (businessType != null && !businessType.isBlank()) q.eq(SysOperationLog::getBusinessType, businessType.trim());

        LocalDateTime start = parseDateTime(startTime, false);
        LocalDateTime end = parseDateTime(endTime, true);
        if (start != null) q.ge(SysOperationLog::getCreateTime, start);
        if (end != null) q.le(SysOperationLog::getCreateTime, end);
        return q;
    }

    private LocalDateTime parseDateTime(String raw, boolean endOfDay) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        try {
            if (value.length() == 10) {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
            }
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignore) {
            return null;
        }
    }
}
