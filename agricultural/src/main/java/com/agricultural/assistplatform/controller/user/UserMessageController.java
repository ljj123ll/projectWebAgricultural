package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.annotation.RequireLoginType;
import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.UserMessage;
import com.agricultural.assistplatform.service.common.UserMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-我的消息")
@RestController
@RequestMapping("/user/messages")
@RequireLoginType("user")
@RequiredArgsConstructor
public class UserMessageController {

    private final UserMessageService userMessageService;

    @Operation(summary = "消息列表")
    @GetMapping
    public Result<PageResult<UserMessage>> list(
            @RequestParam(required = false) Integer senderType,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = LoginContext.getUserId();
        return Result.ok(userMessageService.list(userId, senderType, isRead, pageNum, pageSize));
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = LoginContext.getUserId();
        return Result.ok(userMessageService.unreadCount(userId));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = LoginContext.getUserId();
        userMessageService.markRead(userId, id);
        return Result.ok();
    }

    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = LoginContext.getUserId();
        userMessageService.markAllRead(userId);
        return Result.ok();
    }
}
