package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.dto.user.*;
import com.agricultural.assistplatform.service.user.UserAuthService;
import com.agricultural.assistplatform.vo.user.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端 - 登录注册模块
 * POST /user/sms/send, /user/register, /user/login/sms, /user/login/password, PUT /user/password/reset, POST /user/logout
 */
@Tag(name = "用户端-登录注册")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public Result<Void> sendSms(@RequestBody java.util.Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) {
            return Result.fail(400, "手机号不能为空");
        }
        userAuthService.sendSms(phone);
        return Result.ok();
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        return Result.ok(userAuthService.register(dto));
    }

    @Operation(summary = "验证码登录")
    @PostMapping("/login/sms")
    public Result<LoginVO> loginSms(@Valid @RequestBody SmsLoginDTO dto) {
        return Result.ok(userAuthService.loginBySms(dto));
    }

    @Operation(summary = "密码登录")
    @PostMapping("/login/password")
    public Result<LoginVO> loginPassword(@Valid @RequestBody PasswordLoginDTO dto) {
        return Result.ok(userAuthService.loginByPassword(dto));
    }

    @Operation(summary = "重置密码")
    @PutMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userAuthService.resetPassword(dto);
        return Result.ok();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
