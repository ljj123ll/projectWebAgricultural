package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.annotation.AdminPermission;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.AfterSale;
import com.agricultural.assistplatform.service.admin.AdminAfterSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员端-售后管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@AdminPermission("after_sale:manage")
public class AdminAfterSaleController {

    private final AdminAfterSaleService adminAfterSaleService;

    @Operation(summary = "售后列表")
    @GetMapping("/after-sale")
    public Result<PageResult<AfterSale>> list(
            @RequestParam(required = false) Integer afterSaleStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminAfterSaleService.list(afterSaleStatus, pageNum, pageSize));
    }

    @Operation(summary = "处理售后")
    @PutMapping("/after-sale/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminAfterSaleService.handle(id, body);
        return Result.ok();
    }
}
