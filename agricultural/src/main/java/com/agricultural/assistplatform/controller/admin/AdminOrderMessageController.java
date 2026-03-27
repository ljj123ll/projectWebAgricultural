package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.annotation.AdminPermission;
import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.OrderCommunication;
import com.agricultural.assistplatform.service.common.OrderCommunicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员端-订单沟通")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@AdminPermission("order:manage")
public class AdminOrderMessageController {

    private final OrderCommunicationService orderCommunicationService;

    @Operation(summary = "订单沟通历史消息")
    @GetMapping("/orders/{orderNo}/messages")
    public Result<PageResult<OrderCommunication>> list(
            @PathVariable String orderNo,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        return Result.ok(orderCommunicationService.list(orderNo, LoginContext.getUserId(), "admin", pageNum, pageSize));
    }
}
