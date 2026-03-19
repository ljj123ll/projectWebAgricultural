package com.agricultural.assistplatform.controller.common;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.AfterSaleCommunication;
import com.agricultural.assistplatform.service.common.AfterSaleCommunicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 售后消息控制器
 */
@RestController
@RequestMapping("/common/after-sale")
@Tag(name = "售后沟通模块")
@RequiredArgsConstructor
public class AfterSaleMessageController {

    private final AfterSaleCommunicationService communicationService;

    @GetMapping("/{afterSaleNo}/messages")
    @Operation(summary = "获取售后消息列表")
    public Result<List<AfterSaleCommunication>> getMessages(
            @PathVariable String afterSaleNo,
            @RequestParam(required = false) Long lastId) {
        List<AfterSaleCommunication> messages = communicationService.getMessages(afterSaleNo, lastId);
        return Result.ok(messages);
    }

    @PostMapping("/{afterSaleNo}/messages")
    @Operation(summary = "发送售后消息")
    public Result<String> sendMessage(
            @PathVariable String afterSaleNo,
            @RequestBody MessageRequest request) {
        communicationService.sendMessage(afterSaleNo, request.getSenderType(), 
                request.getSenderId(), request.getContent());
        return Result.ok("发送成功");
    }

    @GetMapping("/{afterSaleNo}/messages/unread")
    @Operation(summary = "获取未读消息数")
    public Result<Integer> getUnreadCount(
            @PathVariable String afterSaleNo,
            @RequestParam Integer senderType) {
        int count = communicationService.getUnreadCount(afterSaleNo, senderType);
        return Result.ok(count);
    }

    @PutMapping("/{afterSaleNo}/messages/read")
    @Operation(summary = "标记消息为已读")
    public Result<String> markAsRead(
            @PathVariable String afterSaleNo,
            @RequestParam Integer senderType) {
        communicationService.markAsRead(afterSaleNo, senderType);
        return Result.ok("标记成功");
    }

    /**
     * 消息请求DTO
     */
    public static class MessageRequest {
        private Integer senderType;
        private Long senderId;
        private String content;

        public Integer getSenderType() {
            return senderType;
        }

        public void setSenderType(Integer senderType) {
            this.senderType = senderType;
        }

        public Long getSenderId() {
            return senderId;
        }

        public void setSenderId(Long senderId) {
            this.senderId = senderId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
