package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.dto.user.CreateOrderDTO;
import com.agricultural.assistplatform.service.user.UserOrderService;
import com.agricultural.assistplatform.vo.user.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-订单")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserOrderController {

    private final UserOrderService userOrderService;

    @Operation(summary = "创建订单")
    @PostMapping("/orders")
    public Result<OrderVO> create(@Valid @RequestBody CreateOrderDTO dto) {
        return Result.ok(userOrderService.create(dto));
    }

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public Result<PageResult<OrderVO>> list(
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(userOrderService.list(orderStatus, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public Result<OrderVO> get(@PathVariable Long id) {
        return Result.ok(userOrderService.getById(id));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/orders/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        userOrderService.cancel(id);
        return Result.ok();
    }

    @Operation(summary = "支付订单（模拟）")
    @PostMapping("/orders/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        userOrderService.pay(id);
        return Result.ok();
    }

    @Operation(summary = "确认收货")
    @PutMapping("/orders/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        userOrderService.receive(id);
        return Result.ok();
    }
}
