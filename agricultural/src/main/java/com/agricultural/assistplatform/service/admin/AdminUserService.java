package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.UserInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.UserInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserInfoMapper userInfoMapper;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    public PageResult<UserInfo> list(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<UserInfo> q = new LambdaQueryWrapper<UserInfo>();
        if (StrUtil.isNotBlank(keyword)) {
            String kw = keyword.trim();
            q.and(w -> w.like(UserInfo::getPhone, kw).or().like(UserInfo::getNickname, kw));
        }
        if (status != null) q.eq(UserInfo::getStatus, status);
        q.orderByDesc(UserInfo::getCreateTime);
        Page<UserInfo> page = userInfoMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public Long create(UserInfo user) {
        if (user == null || StrUtil.isBlank(user.getPhone())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "手机号不能为空");
        }
        String phone = user.getPhone().trim();
        Long exists = userInfoMapper.selectCount(
                new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getPhone, phone));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该手机号已存在");
        }

        UserInfo insert = new UserInfo();
        insert.setPhone(phone);
        insert.setNickname(StrUtil.isNotBlank(user.getNickname()) ? user.getNickname().trim() : maskPhone(phone));
        insert.setAvatarUrl(user.getAvatarUrl());
        insert.setStatus(user.getStatus() != null ? user.getStatus() : 1);
        if (StrUtil.isNotBlank(user.getPassword())) {
            insert.setPassword(PASSWORD_ENCODER.encode(user.getPassword().trim()));
        }

        userInfoMapper.insert(insert);
        return insert.getId();
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

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
