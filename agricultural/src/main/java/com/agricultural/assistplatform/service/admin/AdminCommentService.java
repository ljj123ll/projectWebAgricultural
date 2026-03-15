package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.Comment;
import com.agricultural.assistplatform.mapper.CommentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminCommentService {

    private final CommentMapper commentMapper;

    public PageResult<Comment> list(Long productId, Long userId, Integer auditStatus, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<Comment> q = new LambdaQueryWrapper<Comment>();
        if (productId != null) q.eq(Comment::getProductId, productId);
        if (userId != null) q.eq(Comment::getUserId, userId);
        if (auditStatus != null) q.eq(Comment::getAuditStatus, auditStatus);
        q.orderByDesc(Comment::getCreateTime);
        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void audit(Long id, Integer auditStatus) {
        Comment c = new Comment();
        c.setId(id);
        c.setAuditStatus(auditStatus);
        commentMapper.updateById(c);
    }

    public void delete(Long id) {
        commentMapper.deleteById(id);
    }
}
