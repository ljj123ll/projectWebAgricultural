package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.annotation.AdminPermission;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.AuditRecord;
import com.agricultural.assistplatform.service.admin.AdminAuditRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员端-审核记录")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@AdminPermission("audit_record:view")
public class AdminAuditRecordController {

    private final AdminAuditRecordService adminAuditRecordService;

    @Operation(summary = "审核记录列表")
    @GetMapping("/audit-records")
    public Result<PageResult<AuditRecord>> list(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminAuditRecordService.list(businessType, auditStatus, pageNum, pageSize));
    }
}
