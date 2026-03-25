package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.Comment;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.CommentMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.service.common.UserMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCommentService {

    private final CommentMapper commentMapper;
    private final ProductInfoMapper productInfoMapper;
    private final UserMessageService userMessageService;

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

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, Integer auditStatus) {
        if (id == null) throw new BusinessException(ResultCode.BAD_REQUEST, "评论ID不能为空");
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "审核状态仅支持通过或不通过");
        }

        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");

        comment.setAuditStatus(auditStatus);
        commentMapper.updateById(comment);

        // 审核后同步刷新商品评分，仅统计已通过评论
        updateProductScore(comment.getProductId());

        if (auditStatus == 1) {
            userMessageService.createAdminMessage(
                    comment.getUserId(),
                    "评论审核通过",
                    "您的评论审核已通过，现已展示在商品评论区。",
                    "product",
                    String.valueOf(comment.getProductId())
            );
        } else {
            userMessageService.createAdminMessage(
                    comment.getUserId(),
                    "评论审核未通过",
                    "用户该评论违规，审核未通过",
                    "comment",
                    String.valueOf(comment.getId())
            );
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) return;
        commentMapper.deleteById(id);
        updateProductScore(comment.getProductId());
    }

    private void updateProductScore(Long productId) {
        if (productId == null) return;
        List<Comment> list = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getProductId, productId)
                .eq(Comment::getAuditStatus, 1));

        BigDecimal score = list.isEmpty()
                ? BigDecimal.valueOf(5.0)
                : BigDecimal.valueOf(list.stream().mapToInt(Comment::getScore).average().orElse(5.0));

        productInfoMapper.update(null, new LambdaUpdateWrapper<ProductInfo>()
                .eq(ProductInfo::getId, productId)
                .set(ProductInfo::getScore, score));
    }
}
