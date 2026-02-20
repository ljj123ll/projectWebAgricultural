package com.agricultural.assistplatform.controller.merchant;

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
@RequiredArgsConstructor
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

    @Operation(summary = "发货")
    @PutMapping("/orders/{id}/ship")
    public Result<Void> ship(@PathVariable Long id, @RequestBody Map<String, String> body) {
        merchantOrderService.ship(id, body);
        return Result.ok();
    }
}
