package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.annotation.AdminPermission;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.SysOperationLog;
import com.agricultural.assistplatform.service.admin.AdminOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "管理员端-系统日志")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@AdminPermission("log:view")
public class AdminOperationLogController {

    private final AdminOperationLogService adminOperationLogService;

    @Operation(summary = "日志列表")
    @GetMapping("/operation-logs")
    public Result<PageResult<SysOperationLog>> list(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminOperationLogService.list(operationType, businessType, startTime, endTime, pageNum, pageSize));
    }

    @Operation(summary = "日志导出（CSV）")
    @GetMapping("/operation-logs/export")
    public void export(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletResponse response
    ) throws Exception {
        List<SysOperationLog> logs = adminOperationLogService.listForExport(operationType, businessType, startTime, endTime);
        String fileName = "operation_logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        response.getWriter().write('\ufeff'); // BOM
        response.getWriter().write("ID,操作人ID,操作人,操作类型,业务类型,业务ID,操作内容,IP,时间\n");
        for (SysOperationLog log : logs) {
            response.getWriter().write(csv(log.getId()) + ","
                    + csv(log.getOperatorId()) + ","
                    + csv(log.getOperatorName()) + ","
                    + csv(log.getOperationType()) + ","
                    + csv(log.getBusinessType()) + ","
                    + csv(log.getBusinessId()) + ","
                    + csv(log.getOperationContent()) + ","
                    + csv(log.getIp()) + ","
                    + csv(log.getCreateTime()) + "\n");
        }
        response.getWriter().flush();
    }

    private String csv(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
