package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.service.admin.AdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员端-审核")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @Operation(summary = "商家审核列表")
    @GetMapping("/merchants/audit")
    public Result<PageResult<MerchantInfo>> merchantAuditList(
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminAuditService.merchantAuditList(auditStatus, pageNum, pageSize));
    }

    @Operation(summary = "审核商家")
    @PutMapping("/merchants/{id}/audit")
    public Result<Void> auditMerchant(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminAuditService.auditMerchant(id, body);
        return Result.ok();
    }

    @Operation(summary = "商品审核列表")
    @GetMapping("/products/audit")
    public Result<PageResult<ProductInfo>> productAuditList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminAuditService.productAuditList(status, pageNum, pageSize));
    }

    @Operation(summary = "审核商品")
    @PutMapping("/products/{id}/audit")
    public Result<Void> auditProduct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminAuditService.auditProduct(id, body);
        return Result.ok();
    }
}
