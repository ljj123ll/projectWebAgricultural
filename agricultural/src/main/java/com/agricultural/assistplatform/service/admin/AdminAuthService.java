package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.SysUser;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.SysUserMapper;
import com.agricultural.assistplatform.service.SmsService;
import com.agricultural.assistplatform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final SysUserMapper sysUserMapper;
    private final SmsService smsService;
    private final JwtUtil jwtUtil;
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String code = body.get("code");
        String phone = body.get("phone");
        if (username == null || password == null) throw new BusinessException(ResultCode.BAD_REQUEST, "账号密码不能为空");
        if (code == null || phone == null || !smsService.verifyCode(phone, code))
            throw new BusinessException(ResultCode.BAD_REQUEST, "短信验证码错误或已过期");
        SysUser admin = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
        if (admin == null || admin.getStatus() != 1) throw new BusinessException(ResultCode.BAD_REQUEST, "账号不存在或已禁用");
        if (!encoder.matches(password, admin.getPassword())) throw new BusinessException(ResultCode.BAD_REQUEST, "密码错误");
        admin.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(admin);
        String token = jwtUtil.generateToken(String.valueOf(admin.getId()), "admin");
        return Map.of(
                "token", token,
                "adminId", admin.getId(),
                "username", admin.getUsername(),
                "roleId", admin.getRoleId());
    }
}
