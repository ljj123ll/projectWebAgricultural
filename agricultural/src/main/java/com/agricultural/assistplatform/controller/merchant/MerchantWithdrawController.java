package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.annotation.RequireLoginType;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.dto.merchant.MerchantWithdrawApplyDTO;
import com.agricultural.assistplatform.entity.MerchantWithdraw;
import com.agricultural.assistplatform.service.merchant.MerchantWithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "商家端-提现")
@RestController
@RequestMapping("/merchant")
@RequireLoginType("merchant")
@RequiredArgsConstructor
public class MerchantWithdrawController {

    private final MerchantWithdrawService merchantWithdrawService;

    @Operation(summary = "申请提现")
    @PostMapping("/withdrawals")
    public Result<Map<String, String>> apply(@RequestBody MerchantWithdrawApplyDTO body) {
        String withdrawNo = merchantWithdrawService.apply(body);
        return Result.ok(Map.of("withdrawNo", withdrawNo));
    }

    @Operation(summary = "提现记录")
    @GetMapping("/withdrawals")
    public Result<PageResult<MerchantWithdraw>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.ok(merchantWithdrawService.list(status, keyword, startDate, endDate, pageNum, pageSize));
    }

    @Operation(summary = "取消提现申请")
    @PutMapping("/withdrawals/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        merchantWithdrawService.cancel(id);
        return Result.ok();
    }

    @Operation(summary = "提现可用余额")
    @GetMapping("/withdrawals/available")
    public Result<Map<String, BigDecimal>> available() {
        return Result.ok(merchantWithdrawService.available());
    }
}
