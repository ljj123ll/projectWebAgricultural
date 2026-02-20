package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.merchant.MerchantAfterSaleService;
import com.agricultural.assistplatform.vo.user.AfterSaleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "商家端-售后")
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantAfterSaleController {

    private final MerchantAfterSaleService merchantAfterSaleService;

    @Operation(summary = "售后列表")
    @GetMapping("/after-sale")
    public Result<PageResult<AfterSaleVO>> list(
            @RequestParam(required = false) Integer afterSaleStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(merchantAfterSaleService.list(afterSaleStatus, pageNum, pageSize));
    }

    @Operation(summary = "处理售后")
    @PutMapping("/after-sale/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        merchantAfterSaleService.handle(id, body);
        return Result.ok();
    }
}
