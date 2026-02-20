package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.dto.user.*;
import com.agricultural.assistplatform.entity.UserInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.UserInfoMapper;
import com.agricultural.assistplatform.service.SmsService;
import com.agricultural.assistplatform.util.JwtUtil;
import com.agricultural.assistplatform.vo.user.LoginVO;
import com.agricultural.assistplatform.vo.user.UserInfoVO;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserAuthService {

    private final UserInfoMapper userInfoMapper;
    private final SmsService smsService;
    private final JwtUtil jwtUtil;
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public void sendSms(String phone) {
        smsService.sendCode(phone);
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(UserRegisterDTO dto) {
        if (!smsService.verifyCode(dto.getPhone(), dto.getCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }
        Long cnt = userInfoMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getPhone, dto.getPhone()));
        if (cnt > 0) throw new BusinessException(ResultCode.BAD_REQUEST, "该手机号已注册");
        UserInfo user = new UserInfo();
        user.setPhone(dto.getPhone());
        user.setNickname(StrUtil.isNotBlank(dto.getNickname()) ? dto.getNickname() : maskPhone(dto.getPhone()));
        user.setPassword(StrUtil.isNotBlank(dto.getPassword()) ? encoder.encode(dto.getPassword()) : null);
        user.setStatus(1);
        userInfoMapper.insert(user);
        return buildLoginVO(user);
    }

    public LoginVO loginBySms(SmsLoginDTO dto) {
        if (!smsService.verifyCode(dto.getPhone(), dto.getCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }
        UserInfo user = userInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getPhone, dto.getPhone()));
        if (user == null) throw new BusinessException(ResultCode.BAD_REQUEST, "请先注册");
        if (user.getStatus() != 1) throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        user.setLastLoginTime(LocalDateTime.now());
        userInfoMapper.updateById(user);
        return buildLoginVO(user);
    }

    public LoginVO loginByPassword(PasswordLoginDTO dto) {
        UserInfo user = userInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getPhone, dto.getPhone()));
        if (user == null) throw new BusinessException(ResultCode.BAD_REQUEST, "用户不存在");
        if (user.getStatus() != 1) throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        if (StrUtil.isBlank(user.getPassword())) throw new BusinessException(ResultCode.BAD_REQUEST, "请使用验证码登录");
        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码错误");
        }
        user.setLastLoginTime(LocalDateTime.now());
        userInfoMapper.updateById(user);
        return buildLoginVO(user);
    }

    public void resetPassword(ResetPasswordDTO dto) {
        if (!smsService.verifyCode(dto.getPhone(), dto.getCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }
        UserInfo user = userInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getPhone, dto.getPhone()));
        if (user == null) throw new BusinessException(ResultCode.BAD_REQUEST, "用户不存在");
        user.setPassword(encoder.encode(dto.getNewPassword()));
        userInfoMapper.updateById(user);
    }

    private LoginVO buildLoginVO(UserInfo user) {
        String token = jwtUtil.generateToken(String.valueOf(user.getId()), "user");
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(vo);
        return loginVO;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
