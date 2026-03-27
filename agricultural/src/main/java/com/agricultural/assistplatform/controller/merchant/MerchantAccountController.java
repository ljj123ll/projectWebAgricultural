package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.annotation.RequireLoginType;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.MerchantAccount;
import com.agricultural.assistplatform.service.merchant.MerchantAccountService;
import com.agricultural.assistplatform.vo.merchant.MerchantAccountOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "商家端-收款账户")
@RestController
@RequestMapping("/merchant")
@RequireLoginType("merchant")
@RequiredArgsConstructor
public class MerchantAccountController {

    private final MerchantAccountService merchantAccountService;

    @Operation(summary = "账户列表")
    @GetMapping("/accounts")
    public Result<List<MerchantAccount>> list() {
        return Result.ok(merchantAccountService.list());
    }

    @Operation(summary = "账户总览（对账与补贴）")
    @GetMapping("/account/overview")
    public Result<MerchantAccountOverviewVO> overview() {
        return Result.ok(merchantAccountService.overview());
    }

    @Operation(summary = "新增/更新账户")
    @PostMapping("/accounts")
    public Result<Void> save(@RequestBody MerchantAccount body) {
        merchantAccountService.save(body);
        return Result.ok();
    }

    @Operation(summary = "发起1分钱验证（模拟）")
    @PostMapping("/accounts/{id}/verify/init")
    public Result<Map<String, Object>> initVerify(@PathVariable Long id) {
        BigDecimal verifyAmount = merchantAccountService.initVerify(id);
        return Result.ok(Map.of(
                "verifyAmount", verifyAmount,
                "message", "已向该账户发起1分钱打款验证（模拟），请填写到账金额完成验证"
        ));
    }

    @Operation(summary = "确认1分钱验证金额")
    @PostMapping("/accounts/{id}/verify/confirm")
    public Result<Void> confirmVerify(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object raw = body == null ? null : body.get("amount");
        BigDecimal amount = raw == null ? null : new BigDecimal(String.valueOf(raw));
        merchantAccountService.confirmVerify(id, amount);
        return Result.ok();
    }

    @Operation(summary = "提交账户审核")
    @PostMapping("/accounts/{id}/submit-audit")
    public Result<Void> submitAudit(@PathVariable Long id) {
        merchantAccountService.submitAudit(id);
        return Result.ok();
    }
}
