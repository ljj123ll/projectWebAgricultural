package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.entity.SysPermission;
import com.agricultural.assistplatform.entity.SysRole;
import com.agricultural.assistplatform.entity.SysRolePermission;
import com.agricultural.assistplatform.mapper.SysPermissionMapper;
import com.agricultural.assistplatform.mapper.SysRoleMapper;
import com.agricultural.assistplatform.mapper.SysRolePermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final AdminPermissionService adminPermissionService;

    public List<SysRole> listRoles() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    public List<SysPermission> listPermissions() {
        return sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getId));
    }

    public List<Long> getRolePermissionIds(Long roleId) {
        return sysRolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId))
                .stream().map(SysRolePermission::getPermId).collect(Collectors.toList());
    }

    public void updateRolePermissions(Long roleId, List<Long> permIds) {
        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        if (permIds != null && !permIds.isEmpty()) {
            for (Long permId : permIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermId(permId);
                sysRolePermissionMapper.insert(rp);
            }
        }
        adminPermissionService.clearRoleCache(roleId);
    }
}
