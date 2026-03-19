package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.service.merchant.MerchantShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商家端-店铺信息")
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantShopController {

    private final MerchantShopService merchantShopService;

    @Operation(summary = "店铺信息")
    @GetMapping("/shop")
    public Result<com.agricultural.assistplatform.vo.merchant.ShopDetailVO> get() {
        return Result.ok(merchantShopService.get());
    }

    @Operation(summary = "更新店铺信息")
    @PutMapping("/shop")
    public Result<Void> update(@RequestBody ShopInfo body) {
        merchantShopService.update(body);
        return Result.ok();
    }
}
