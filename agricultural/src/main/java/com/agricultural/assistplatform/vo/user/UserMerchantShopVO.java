package com.agricultural.assistplatform.vo.user;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserMerchantShopVO {
    private Long id;
    private Long merchantId;
    private String shopName;
    private String shopIntro;
    private String qualificationImg;
    private String shopType;
    private String categories;
    private String shopAddress;
    private Long productCount;
    private Long commentCount;
    private Integer reviewCount;
    private Integer totalSalesVolume;
    private BigDecimal averageScore;
}
