package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.SysPermission;
import com.agricultural.assistplatform.entity.SysRole;
import com.agricultural.assistplatform.service.admin.AdminRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理员端-角色权限")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @Operation(summary = "角色列表")
    @GetMapping("/roles")
    public Result<List<SysRole>> roles() {
        return Result.ok(adminRoleService.listRoles());
    }

    @Operation(summary = "权限列表")
    @GetMapping("/permissions")
    public Result<List<SysPermission>> permissions() {
        return Result.ok(adminRoleService.listPermissions());
    }

    @Operation(summary = "角色权限")
    @GetMapping("/roles/{id}/permissions")
    public Result<List<Long>> rolePermissions(@PathVariable Long id) {
        return Result.ok(adminRoleService.getRolePermissionIds(id));
    }

    @Operation(summary = "更新角色权限")
    @PutMapping("/roles/{id}/permissions")
    public Result<Void> updateRolePermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        List<Long> permIds = body != null ? body.get("permIds") : null;
        adminRoleService.updateRolePermissions(id, permIds);
        return Result.ok();
    }
}
