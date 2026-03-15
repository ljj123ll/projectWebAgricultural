package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.AfterSaleCommunication;
import com.agricultural.assistplatform.service.common.AfterSaleCommunicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "商家端-售后沟通")
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantAfterSaleMessageController {

    private final AfterSaleCommunicationService afterSaleCommunicationService;

    @Operation(summary = "沟通列表")
    @GetMapping("/after-sale/{no}/messages")
    public Result<PageResult<AfterSaleCommunication>> list(
            @PathVariable String no,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(afterSaleCommunicationService.list(no, pageNum, pageSize));
    }

    @Operation(summary = "发送消息")
    @PostMapping("/after-sale/{no}/messages")
    public Result<Void> send(@PathVariable String no, @RequestBody Map<String, String> body) {
        Long merchantId = LoginContext.getUserId();
        String content = body != null ? body.get("content") : null;
        afterSaleCommunicationService.send(no, 2, merchantId, content);
        return Result.ok();
    }
}
