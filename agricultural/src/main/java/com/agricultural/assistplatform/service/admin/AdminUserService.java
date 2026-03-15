package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.UserInfo;
import com.agricultural.assistplatform.mapper.UserInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserInfoMapper userInfoMapper;

    public PageResult<UserInfo> list(Integer status, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<UserInfo> q = new LambdaQueryWrapper<UserInfo>();
        if (status != null) q.eq(UserInfo::getStatus, status);
        q.orderByDesc(UserInfo::getCreateTime);
        Page<UserInfo> page = userInfoMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public Long create(UserInfo user) {
        userInfoMapper.insert(user);
        return user.getId();
    }

    public void updateStatus(Long id, Integer status) {
        UserInfo u = new UserInfo();
        u.setId(id);
        u.setStatus(status);
        userInfoMapper.updateById(u);
    }

    public void delete(Long id) {
        userInfoMapper.deleteById(id);
    }
}
