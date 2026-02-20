package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.dto.merchant.MerchantRegisterDTO;
import com.agricultural.assistplatform.service.merchant.MerchantAuthService;
import com.agricultural.assistplatform.vo.merchant.MerchantLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "商家端-登录审核")
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final MerchantAuthService merchantAuthService;

    @Operation(summary = "商家注册")
    @PostMapping("/register")
    public Result<MerchantLoginVO> register(@Valid @RequestBody MerchantRegisterDTO dto) {
        return Result.ok(merchantAuthService.register(dto));
    }

    @Operation(summary = "商家登录")
    @PostMapping("/login")
    public Result<MerchantLoginVO> login(@RequestBody Map<String, String> body) {
        return Result.ok(merchantAuthService.login(body));
    }

    @Operation(summary = "审核状态")
    @GetMapping("/audit/status")
    public Result<Map<String, Object>> auditStatus() {
        return Result.ok(merchantAuthService.auditStatus());
    }
}
