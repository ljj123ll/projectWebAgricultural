package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.admin.AdminAuthService;
import com.agricultural.assistplatform.service.admin.AdminPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员端-登录")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;

    @Operation(summary = "管理员登录（账号+密码+短信验证码）")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return Result.ok(adminAuthService.login(body));
    }

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public Result<Void> sendSms(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) return Result.fail(400, "手机号不能为空");
        adminAuthService.sendSms(phone);
        return Result.ok();
    }

    @Operation(summary = "当前管理员有效权限")
    @GetMapping("/me/permissions")
    public Result<java.util.Set<String>> currentPermissions() {
        return Result.ok(adminPermissionService.currentPermissionCodes(LoginContext.getUserId(), LoginContext.getLoginType()));
    }
}
