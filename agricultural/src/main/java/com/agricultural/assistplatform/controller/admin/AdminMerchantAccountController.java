package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.annotation.AdminPermission;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.MerchantAccount;
import com.agricultural.assistplatform.service.admin.AdminMerchantAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员端-商家收款账户审核")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@AdminPermission("merchant_account:audit")
public class AdminMerchantAccountController {

    private final AdminMerchantAccountService adminMerchantAccountService;

    @Operation(summary = "收款账户列表")
    @GetMapping("/merchant-accounts")
    public Result<PageResult<MerchantAccount>> list(
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.ok(adminMerchantAccountService.list(auditStatus, pageNum, pageSize));
    }

    @Operation(summary = "审核收款账户")
    @PutMapping("/merchant-accounts/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean approve = body == null ? null : parseBoolean(body.get("approve"));
        String reason = body == null ? null : (body.get("reason") == null ? null : String.valueOf(body.get("reason")));
        adminMerchantAccountService.audit(id, approve, reason);
        return Result.ok();
    }

    private Boolean parseBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        String text = String.valueOf(value).trim();
        if ("1".equals(text) || "true".equalsIgnoreCase(text)) return true;
        if ("0".equals(text) || "false".equalsIgnoreCase(text)) return false;
        return null;
    }
}
