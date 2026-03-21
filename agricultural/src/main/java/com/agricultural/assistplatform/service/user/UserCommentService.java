package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.dto.user.CommentSubmitDTO;
import com.agricultural.assistplatform.entity.Comment;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.entity.UserInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.CommentMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.mapper.UserInfoMapper;
import com.agricultural.assistplatform.vo.user.ProductCommentVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCommentService {

    private final CommentMapper commentMapper;
    private final OrderMainMapper orderMainMapper;
    private final ProductInfoMapper productInfoMapper;
    private final UserInfoMapper userInfoMapper;

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

    public PageResult<Comment> list(Integer pageNum, Integer pageSize) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId).orderByDesc(Comment::getCreateTime));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public PageResult<ProductCommentVO> listByProduct(Long productId, Integer pageNum, Integer pageSize) {
        if (productId == null) throw new BusinessException(ResultCode.BAD_REQUEST, "商品ID不能为空");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        Page<Comment> page = commentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getProductId, productId)
                        .eq(Comment::getAuditStatus, 1)
                        .orderByDesc(Comment::getCreateTime)
        );

        List<Comment> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResult.of(page.getTotal(), Collections.emptyList());
        }

        Set<Long> userIds = records.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserInfo> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>().in(UserInfo::getId, userIds))
                .stream()
                .collect(Collectors.toMap(UserInfo::getId, it -> it));

        List<ProductCommentVO> list = records.stream().map(comment -> {
            ProductCommentVO vo = new ProductCommentVO();
            vo.setId(comment.getId());
            vo.setOrderNo(comment.getOrderNo());
            vo.setProductId(comment.getProductId());
            vo.setUserId(comment.getUserId());
            vo.setScore(comment.getScore());
            vo.setContent(comment.getContent());
            vo.setImgUrls(comment.getImgUrls());
            vo.setCreateTime(comment.getCreateTime());

            UserInfo user = userMap.get(comment.getUserId());
            String nickname = user != null && user.getNickname() != null && !user.getNickname().isBlank()
                    ? user.getNickname()
                    : "用户" + comment.getUserId();
            vo.setNickname(nickname);
            vo.setAvatarUrl(user != null ? user.getAvatarUrl() : null);
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(page.getTotal(), list);
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
