package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.service.admin.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员端-订单管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public Result<PageResult<OrderMain>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminOrderService.list(status, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public Result<OrderMain> get(@PathVariable Long id) {
        return Result.ok(adminOrderService.get(id));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/orders/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        adminOrderService.cancel(id, reason);
        return Result.ok();
    }
}
