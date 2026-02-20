package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.UserInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.UserInfoMapper;
import com.agricultural.assistplatform.vo.user.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserInfoService {

    private final UserInfoMapper userInfoMapper;
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserInfoVO getInfo() {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        UserInfo u = userInfoMapper.selectById(userId);
        if (u == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        UserInfoVO vo = new UserInfoVO();
        vo.setId(u.getId());
        vo.setPhone(u.getPhone());
        vo.setNickname(u.getNickname());
        vo.setAvatarUrl(u.getAvatarUrl());
        return vo;
    }

    public void updateInfo(Map<String, String> body) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        UserInfo u = userInfoMapper.selectById(userId);
        if (u == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        if (StringUtils.hasText(body.get("nickname"))) u.setNickname(body.get("nickname"));
        if (StringUtils.hasText(body.get("avatarUrl"))) u.setAvatarUrl(body.get("avatarUrl"));
        if (StringUtils.hasText(body.get("password"))) u.setPassword(encoder.encode(body.get("password")));
        userInfoMapper.updateById(u);
    }
}
