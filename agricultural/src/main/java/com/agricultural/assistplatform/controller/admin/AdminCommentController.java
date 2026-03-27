package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.annotation.AdminPermission;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.Comment;
import com.agricultural.assistplatform.service.admin.AdminCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员端-评论管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@AdminPermission("comment:manage")
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    @Operation(summary = "评论列表")
    @GetMapping("/comments")
    public Result<PageResult<Comment>> list(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminCommentService.list(productId, userId, auditStatus, pageNum, pageSize));
    }

    @Operation(summary = "审核评论")
    @PutMapping("/comments/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer auditStatus = body != null ? body.get("auditStatus") : null;
        adminCommentService.audit(id, auditStatus);
        return Result.ok();
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminCommentService.delete(id);
        return Result.ok();
    }
}
