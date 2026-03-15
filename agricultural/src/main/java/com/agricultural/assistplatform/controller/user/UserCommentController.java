package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.dto.user.CommentSubmitDTO;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.Comment;
import com.agricultural.assistplatform.service.user.UserCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-评价")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserCommentController {

    private final UserCommentService userCommentService;

    @Operation(summary = "提交评价")
    @PostMapping("/comments")
    public Result<Void> submit(@Valid @RequestBody CommentSubmitDTO dto) {
        userCommentService.submit(dto);
        return Result.ok();
    }

    @Operation(summary = "评价列表")
    @GetMapping("/comments")
    public Result<PageResult<Comment>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(userCommentService.list(pageNum, pageSize));
    }
}
