package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.SysOperationLog;
import com.agricultural.assistplatform.service.admin.AdminOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员端-系统日志")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final AdminOperationLogService adminOperationLogService;

    @Operation(summary = "日志列表")
    @GetMapping("/operation-logs")
    public Result<PageResult<SysOperationLog>> list(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String businessType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminOperationLogService.list(operationType, businessType, pageNum, pageSize));
    }
}
