package com.agricultural.assistplatform.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantCommentVO {
    private Long id;
    private String orderNo;
    private Long productId;
    private String productName;
    private String productImg;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer score;
    private String content;
    private String imgUrls;
    private String mediaUrls;
    private LocalDateTime createTime;
}
