package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.SysUser;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.SysUserMapper;
import com.agricultural.assistplatform.service.SmsService;
import com.agricultural.assistplatform.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
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
        
        // 允许使用 123456 作为通用测试验证码，或者验证 Redis 中的验证码
        boolean isDevCode = "123456".equals(code);
        if (!isDevCode && (code == null || phone == null || !smsService.verifyCode(phone, code)))
            throw new BusinessException(ResultCode.BAD_REQUEST, "短信验证码错误或已过期");
            
        List<SysUser> adminCandidates = sysUserMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getDeleteFlag, 0)
                        .orderByDesc(SysUser::getUpdateTime)
                        .orderByDesc(SysUser::getId));
        if (adminCandidates == null || adminCandidates.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号不存在或已禁用");
        }

        // 安全手机号必须匹配
        if (phone != null && !phone.isBlank()) {
            adminCandidates = adminCandidates.stream()
                    .filter(item -> phone.equals(item.getPhone()))
                    .toList();
            if (adminCandidates.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "安全手机号不匹配");
            }
        }

        if (adminCandidates.size() > 1) {
            log.warn("检测到重复管理员账号，username={}, 命中{}条，默认使用最新记录id={}",
                    username, adminCandidates.size(), adminCandidates.get(0).getId());
        }

        List<SysUser> activeAdmins = adminCandidates.stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == 1)
                .toList();
        if (activeAdmins.isEmpty()) throw new BusinessException(ResultCode.BAD_REQUEST, "账号不存在或已禁用");

        SysUser admin = activeAdmins.stream()
                .filter(item -> encoder.matches(password, item.getPassword()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "密码错误"));
        admin.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(admin);
        String token = jwtUtil.generateToken(String.valueOf(admin.getId()), "admin");
        
        Map<String, Object> userInfo = new java.util.HashMap<>();
        userInfo.put("id", admin.getId());
        userInfo.put("phone", admin.getPhone());
        userInfo.put("nickname", admin.getRealName() != null ? admin.getRealName() : admin.getUsername());
        userInfo.put("role", "admin");
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return result;
    }

    public void sendSms(String phone) {
        smsService.sendCode(phone);
    }
}
