package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.annotation.RequireLoginType;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.merchant.MerchantOrderService;
import com.agricultural.assistplatform.vo.merchant.MerchantOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "商家端-订单")
@RestController
@RequestMapping("/merchant")
@RequireLoginType("merchant")
@RequiredArgsConstructor
/**
 * 商家订单控制器。
 * 商家发货、查看订单、取消待发货订单都从这里进入服务层。
 */
public class MerchantOrderController {

    private final MerchantOrderService merchantOrderService;

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public Result<PageResult<MerchantOrderVO>> list(
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(merchantOrderService.list(orderStatus, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public Result<MerchantOrderVO> get(@PathVariable Long id) {
        return Result.ok(merchantOrderService.get(id));
    }

    @Operation(summary = "订单详情（按订单号）")
    @GetMapping("/orders/no/{orderNo}")
    public Result<MerchantOrderVO> getByOrderNo(@PathVariable String orderNo) {
        return Result.ok(merchantOrderService.getByOrderNo(orderNo));
    }

    /**
     * 发货入口，要求商家填写物流公司和物流单号。
     */
    @Operation(summary = "发货")
    @PutMapping("/orders/{id}/ship")
    public Result<Void> ship(@PathVariable Long id, @RequestBody Map<String, String> body) {
        merchantOrderService.ship(id, body);
        return Result.ok();
    }

    @Operation(summary = "取消订单")
    @PutMapping("/orders/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        merchantOrderService.cancel(id, reason);
        return Result.ok();
    }
}
