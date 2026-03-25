package com.agricultural.assistplatform.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品详情页评价展示对象
 */
@Data
public class ProductCommentVO {
    private Long id;
    private String orderNo;
    private Long productId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer score;
    private String content;
    private String imgUrls;
    private String mediaUrls;
    private LocalDateTime createTime;
}
