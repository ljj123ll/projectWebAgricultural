package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.annotation.RequireLoginType;
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
@RequireLoginType("user")
@RequiredArgsConstructor
/**
 * 用户端订单控制器。
 * 如果答辩时老师问“前端下单、支付、取消订单最终打到哪个后端入口”，可以从这里开始看。
 */
public class UserOrderController {

    private final UserOrderService userOrderService;

    /**
     * 创建订单入口，兼容购物车下单和直接购买。
     */
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

    /**
     * 模拟支付入口，后续会进入订单状态流转和支付记录幂等处理。
     */
    @Operation(summary = "支付订单（模拟）")
    @PostMapping("/orders/{id}/pay")
    public Result<Void> pay(@PathVariable Long id, @RequestParam(defaultValue = "true") Boolean success) {
        userOrderService.pay(id, success);
        return Result.ok();
    }

    @Operation(summary = "确认收货")
    @PutMapping("/orders/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        userOrderService.receive(id);
        return Result.ok();
    }

    @Operation(summary = "物流信息")
    @GetMapping("/orders/{id}/logistics")
    public Result<com.agricultural.assistplatform.entity.LogisticsInfo> logistics(@PathVariable Long id) {
        return Result.ok(userOrderService.logistics(id));
    }
}
