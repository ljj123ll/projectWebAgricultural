package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.entity.AuditRecord;
import com.agricultural.assistplatform.mapper.AuditRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuditTrailService {

    private final AuditRecordMapper auditRecordMapper;

    public void record(String businessType, Long businessId, boolean approved, String reason) {
        record(businessType, businessId, approved ? 1 : 2, reason);
    }

    public void record(String businessType, Long businessId, Integer auditStatus, String reason) {
        if (!StringUtils.hasText(businessType) || businessId == null || businessId <= 0 || auditStatus == null) {
            return;
        }
        AuditRecord record = new AuditRecord();
        record.setBusinessType(businessType.trim());
        record.setBusinessId(businessId);
        record.setAuditUserId(LoginContext.getUserId() == null ? 0L : LoginContext.getUserId());
        record.setAuditStatus(auditStatus);
        record.setAuditReason(StringUtils.hasText(reason) ? reason.trim() : defaultReason(auditStatus));
        record.setCreateTime(LocalDateTime.now());
        auditRecordMapper.insert(record);
    }

    private String defaultReason(Integer auditStatus) {
        return auditStatus != null && auditStatus == 1 ? "审核通过" : "审核未通过";
    }
}
