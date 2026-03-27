package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.SysPermission;
import com.agricultural.assistplatform.entity.SysRole;
import com.agricultural.assistplatform.entity.SysRolePermission;
import com.agricultural.assistplatform.entity.SysUser;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.SysPermissionMapper;
import com.agricultural.assistplatform.mapper.SysRoleMapper;
import com.agricultural.assistplatform.mapper.SysRolePermissionMapper;
import com.agricultural.assistplatform.mapper.SysUserMapper;
import com.agricultural.assistplatform.service.common.RedisCacheService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private static final String ADMIN_PERMISSION_CACHE_PREFIX = "cache:admin:permissions:";
    private static final Duration ADMIN_PERMISSION_TTL = Duration.ofMinutes(30);

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final RedisCacheService redisCacheService;

    public void assertAdminPermission(Long adminId, String loginType, String permissionCode) {
        if (adminId == null || !"admin".equalsIgnoreCase(String.valueOf(loginType))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先以管理员身份登录");
        }
        AdminPermissionSnapshot snapshot = getSnapshot(adminId);
        if (!snapshot.active) {
            throw new BusinessException(ResultCode.FORBIDDEN, "管理员账号已被禁用");
        }
        if (!StringUtils.hasText(permissionCode)) {
            return;
        }
        if (!hasPermission(snapshot, permissionCode.trim().toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前管理员角色无权访问该功能");
        }
    }

    public void clearAdminCache(Long adminId) {
        if (adminId != null) {
            redisCacheService.delete(ADMIN_PERMISSION_CACHE_PREFIX + adminId);
        }
    }

    public void clearRoleCache(Long roleId) {
        if (roleId == null) {
            return;
        }
        List<SysUser> admins = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, roleId)
                .eq(SysUser::getDeleteFlag, 0));
        admins.forEach(admin -> clearAdminCache(admin.getId()));
    }

    private AdminPermissionSnapshot getSnapshot(Long adminId) {
        return redisCacheService.getOrLoad(
                ADMIN_PERMISSION_CACHE_PREFIX + adminId,
                ADMIN_PERMISSION_TTL,
                new TypeReference<>() {
                },
                () -> loadSnapshot(adminId)
        );
    }

    private AdminPermissionSnapshot loadSnapshot(Long adminId) {
        SysUser admin = sysUserMapper.selectById(adminId);
        if (admin == null || (admin.getDeleteFlag() != null && admin.getDeleteFlag() == 1)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "管理员账号不存在");
        }

        AdminPermissionSnapshot snapshot = new AdminPermissionSnapshot();
        snapshot.setAdminId(admin.getId());
        snapshot.setRoleId(admin.getRoleId());
        snapshot.setActive(admin.getStatus() != null && admin.getStatus() == 1);
        if (admin.getRoleId() == null) {
            snapshot.setRoleCode("super_admin");
            snapshot.setPermissionCodes(Set.of("*"));
            return snapshot;
        }

        SysRole role = sysRoleMapper.selectById(admin.getRoleId());
        snapshot.setRoleCode(role != null ? role.getRoleCode() : null);

        List<Long> permIds = sysRolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, admin.getRoleId()))
                .stream()
                .map(SysRolePermission::getPermId)
                .toList();
        if (permIds.isEmpty()) {
            snapshot.setPermissionCodes(Set.of());
            return snapshot;
        }

        Set<String> permissionCodes = new HashSet<>();
        sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>().in(SysPermission::getId, permIds))
                .forEach(permission -> {
                    if (StringUtils.hasText(permission.getPermCode())) {
                        permissionCodes.add(permission.getPermCode().trim().toLowerCase(Locale.ROOT));
                    }
                });
        snapshot.setPermissionCodes(permissionCodes);
        return snapshot;
    }

    private boolean hasPermission(AdminPermissionSnapshot snapshot, String permissionCode) {
        String roleCode = snapshot.roleCode == null ? "" : snapshot.roleCode.trim().toLowerCase(Locale.ROOT);
        if ("super_admin".equals(roleCode) || "admin".equals(roleCode) || "root".equals(roleCode)) {
            return true;
        }

        Set<String> codes = snapshot.permissionCodes == null ? Set.of() : snapshot.permissionCodes;
        if (!codes.isEmpty()) {
            if (codes.contains("*")
                    || codes.contains(permissionCode)
                    || codes.contains(moduleWildcard(permissionCode))
                    || codes.contains(permissionCode.replace(':', '.'))) {
                return true;
            }
        }

        return switch (roleCode) {
            case "content_admin", "content-manager", "editor_admin" ->
                    Set.of("audit:manage", "comment:manage", "news:manage").contains(permissionCode);
            default -> false;
        };
    }

    private String moduleWildcard(String permissionCode) {
        int idx = permissionCode.indexOf(':');
        if (idx < 0) {
            return permissionCode + ":*";
        }
        return permissionCode.substring(0, idx) + ":*";
    }

    @Data
    public static class AdminPermissionSnapshot {
        private Long adminId;
        private Long roleId;
        private String roleCode;
        private boolean active;
        private Set<String> permissionCodes = Set.of();
    }
}
