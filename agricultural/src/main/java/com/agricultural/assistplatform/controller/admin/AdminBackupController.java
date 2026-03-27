package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.annotation.AdminPermission;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.admin.AdminBackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "管理员端-数据备份")
@RestController
@RequestMapping("/admin/backup")
@RequiredArgsConstructor
@AdminPermission("backup:manage")
public class AdminBackupController {

    private final AdminBackupService adminBackupService;

    @Operation(summary = "获取备份列表")
    @GetMapping
    public Result<List<Map<String, Object>>> list() throws IOException {
        return Result.ok(adminBackupService.list());
    }

    @Operation(summary = "手动备份")
    @PostMapping
    public Result<Void> create() throws IOException {
        adminBackupService.createBackup("manual");
        return Result.ok();
    }

    @Operation(summary = "恢复备份")
    @PostMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) throws Exception {
        try {
            adminBackupService.restore(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(404, e.getMessage());
        }
    }

    @Operation(summary = "删除备份")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) throws IOException {
        if (!adminBackupService.delete(id)) {
            return Result.fail(404, "备份文件不存在");
        }
        return Result.ok();
    }
}
