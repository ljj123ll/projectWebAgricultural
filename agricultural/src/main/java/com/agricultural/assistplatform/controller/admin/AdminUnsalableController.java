package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.service.admin.AdminUnsalableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员端-滞销专区")
@RestController
@RequestMapping("/admin/unsalable")
@RequiredArgsConstructor
public class AdminUnsalableController {

    private final AdminUnsalableService adminUnsalableService;

    @Operation(summary = "获取滞销商品列表")
    @GetMapping
    public Result<PageResult<ProductInfo>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminUnsalableService.list(pageNum, pageSize));
    }

    @Operation(summary = "设置/取消滞销商品")
    @PutMapping("/{id}")
    public Result<Void> setUnsalable(@PathVariable Long id, @RequestParam Integer isUnsalable) {
        adminUnsalableService.setUnsalable(id, isUnsalable);
        return Result.ok();
    }
}