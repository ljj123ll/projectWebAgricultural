package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.dto.merchant.ProductSaveDTO;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.service.merchant.MerchantProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "商家端-商品管理")
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantProductController {

    private final MerchantProductService merchantProductService;

    @Operation(summary = "商品列表")
    @GetMapping("/products")
    public Result<PageResult<ProductInfo>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(merchantProductService.list(pageNum, pageSize));
    }

    @Operation(summary = "新增商品")
    @PostMapping("/products")
    public Result<ProductInfo> add(@RequestBody ProductSaveDTO dto) {
        return Result.ok(merchantProductService.add(dto));
    }

    @Operation(summary = "编辑商品")
    @PutMapping("/products/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProductSaveDTO dto) {
        merchantProductService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "生成溯源码")
    @PostMapping("/products/{id}/qrcode")
    public Result<Map<String, String>> qrcode(@PathVariable Long id) {
        String url = merchantProductService.generateQrcode(id);
        return Result.ok(Map.of("qrCodeUrl", url != null ? url : ""));
    }

    @Operation(summary = "上下架")
    @PutMapping("/products/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body != null ? body.get("status") : null;
        if (status == null) return Result.fail(400, "status不能为空");
        merchantProductService.updateStatus(id, status);
        return Result.ok();
    }
}
