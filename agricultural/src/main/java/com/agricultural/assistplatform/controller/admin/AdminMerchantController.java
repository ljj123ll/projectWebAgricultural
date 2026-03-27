package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.annotation.AdminPermission;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.service.admin.AdminMerchantService;
import com.agricultural.assistplatform.vo.admin.AdminMerchantDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员端-商家管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@AdminPermission("merchant:manage")
public class AdminMerchantController {

    private final AdminMerchantService adminMerchantService;

    @Operation(summary = "商家列表")
    @GetMapping("/merchants")
    public Result<PageResult<MerchantInfo>> list(
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminMerchantService.list(auditStatus, status, pageNum, pageSize));
    }

    @Operation(summary = "更新商家状态")
    @PutMapping("/merchants/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body != null ? body.get("status") : null;
        adminMerchantService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "商家详情")
    @GetMapping("/merchants/{id}")
    public Result<AdminMerchantDetailVO> detail(@PathVariable Long id) {
        return Result.ok(adminMerchantService.detail(id));
    }
}
