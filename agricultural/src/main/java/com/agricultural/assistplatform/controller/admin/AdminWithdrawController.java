package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.MerchantWithdraw;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.service.admin.AdminWithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员端-商家提现审核")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWithdrawController {

    private final AdminWithdrawService adminWithdrawService;

    @Operation(summary = "提现申请列表")
    @GetMapping("/withdrawals")
    public Result<PageResult<MerchantWithdraw>> list(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.ok(adminWithdrawService.list(merchantId, status, keyword, startDate, endDate, pageNum, pageSize));
    }

    @Operation(summary = "审核提现申请")
    @PutMapping("/withdrawals/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approve = parseApprove(body == null ? null : body.get("approve"));
        String remark = body == null || body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        Long adminId = LoginContext.getUserId();
        adminWithdrawService.audit(id, approve, remark, adminId);
        return Result.ok();
    }

    @Operation(summary = "人工打款")
    @PostMapping("/withdrawals/{id}/manual-transfer")
    public Result<Void> manualTransfer(@PathVariable Long id) {
        adminWithdrawService.manualTransfer(id);
        return Result.ok();
    }

    private boolean parseApprove(Object value) {
        if (value instanceof Boolean b) return b;
        if (value != null) {
            String text = String.valueOf(value).trim();
            if ("1".equals(text) || "true".equalsIgnoreCase(text)) return true;
            if ("0".equals(text) || "false".equalsIgnoreCase(text)) return false;
        }
        throw new BusinessException(ResultCode.BAD_REQUEST, "审核结果无效");
    }
}

