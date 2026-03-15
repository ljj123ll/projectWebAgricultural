package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.PaymentRecord;
import com.agricultural.assistplatform.service.admin.AdminPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员端-支付与退款")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @Operation(summary = "支付记录列表")
    @GetMapping("/payments")
    public Result<PageResult<PaymentRecord>> list(
            @RequestParam(required = false) Integer payStatus,
            @RequestParam(required = false) Integer refundStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminPaymentService.list(payStatus, refundStatus, pageNum, pageSize));
    }

    @Operation(summary = "更新退款状态")
    @PutMapping("/payments/{id}/refund")
    public Result<Void> updateRefund(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer refundStatus = body != null ? body.get("refundStatus") : null;
        adminPaymentService.updateRefundStatus(id, refundStatus);
        return Result.ok();
    }
}
