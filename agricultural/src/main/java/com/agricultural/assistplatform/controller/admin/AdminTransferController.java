package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.entity.ReconciliationDetail;
import com.agricultural.assistplatform.service.admin.AdminTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员端-资金管控")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminTransferController {

    private final AdminTransferService adminTransferService;

    @Operation(summary = "打款台账")
    @GetMapping("/transfers")
    public Result<PageResult<ReconciliationDetail>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminTransferService.transferList(pageNum, pageSize));
    }

    @Operation(summary = "手动打款")
    @PostMapping("/transfers/{id}/manual")
    public Result<Void> manual(@PathVariable Long id) {
        adminTransferService.manualTransfer(id);
        return Result.ok();
    }

    @Operation(summary = "物流超时监控")
    @GetMapping("/orders/logistics-abnormal")
    public Result<PageResult<OrderMain>> logisticsAbnormal(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminTransferService.logisticsAbnormal(pageNum, pageSize));
    }
}
