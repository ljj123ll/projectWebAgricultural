package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.MerchantAccount;
import com.agricultural.assistplatform.service.merchant.MerchantAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商家端-收款账户")
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantAccountController {

    private final MerchantAccountService merchantAccountService;

    @Operation(summary = "账户列表")
    @GetMapping("/accounts")
    public Result<List<MerchantAccount>> list() {
        return Result.ok(merchantAccountService.list());
    }

    @Operation(summary = "新增/更新账户")
    @PostMapping("/accounts")
    public Result<Void> save(@RequestBody MerchantAccount body) {
        merchantAccountService.save(body);
        return Result.ok();
    }
}
