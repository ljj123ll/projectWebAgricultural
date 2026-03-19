package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "管理员端-数据备份")
@RestController
@RequestMapping("/admin/backup")
public class AdminBackupController {

    // 模拟的备份记录列表
    private static final List<Map<String, Object>> backupList = new ArrayList<>();

    static {
        backupList.add(Map.of(
                "id", 1,
                "fileName", "backup_20240315_020000.sql",
                "size", "256MB",
                "type", "auto",
                "status", 1,
                "createTime", "2024-03-15 02:00:00"
        ));
    }

    @Operation(summary = "获取备份列表")
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(backupList);
    }

    @Operation(summary = "手动备份")
    @PostMapping
    public Result<Void> create() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        backupList.add(0, Map.of(
                "id", backupList.size() + 1,
                "fileName", "backup_" + now + ".sql",
                "size", "12MB",
                "type", "manual",
                "status", 1,
                "createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));
        return Result.ok();
    }

    @Operation(summary = "恢复备份")
    @PostMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        return Result.ok();
    }

    @Operation(summary = "删除备份")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        backupList.removeIf(map -> id.equals(map.get("id")));
        return Result.ok();
    }
}