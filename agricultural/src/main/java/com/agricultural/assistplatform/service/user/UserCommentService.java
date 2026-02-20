package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.dto.user.CommentSubmitDTO;
import com.agricultural.assistplatform.entity.Comment;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.CommentMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCommentService {

    private final CommentMapper commentMapper;
    private final OrderMainMapper orderMainMapper;
    private final ProductInfoMapper productInfoMapper;

    @Transactional(rollbackFor = Exception.class)
    public void submit(CommentSubmitDTO dto) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, dto.getOrderNo()).eq(OrderMain::getUserId, userId));
        if (order == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (order.getOrderStatus() != 4) throw new BusinessException(ResultCode.BAD_REQUEST, "仅已完成订单可评价");
        Long exist = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getOrderNo, dto.getOrderNo()).eq(Comment::getProductId, dto.getProductId()));
        if (exist > 0) throw new BusinessException(ResultCode.BAD_REQUEST, "已评价过该商品");
        Comment c = new Comment();
        c.setOrderNo(dto.getOrderNo());
        c.setProductId(dto.getProductId());
        c.setUserId(userId);
        c.setScore(dto.getScore());
        c.setContent(dto.getContent());
        c.setImgUrls(dto.getImgUrls());
        c.setAuditStatus(1);
        commentMapper.insert(c);
        updateProductScore(dto.getProductId());
    }

    private void updateProductScore(Long productId) {
        List<Comment> list = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getProductId, productId).eq(Comment::getAuditStatus, 1));
        if (list.isEmpty()) return;
        double avg = list.stream().mapToInt(Comment::getScore).average().orElse(5);
        productInfoMapper.update(null, new LambdaUpdateWrapper<com.agricultural.assistplatform.entity.ProductInfo>()
                .eq(com.agricultural.assistplatform.entity.ProductInfo::getId, productId)
                .set(com.agricultural.assistplatform.entity.ProductInfo::getScore, BigDecimal.valueOf(avg)));
    }
}
