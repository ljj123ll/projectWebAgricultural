package com.agricultural.assistplatform.vo.user;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品详情：含溯源信息、商家入口
 */
@Data
public class ProductDetailVO {
    private Long id;
    private String productName;
    private BigDecimal price;
    private Long categoryId;
    private Integer stock;
    private String productImg;
    private String productDesc;
    private String originPlace;
    private Integer salesVolume;
    private BigDecimal score;
    private Long merchantId;
    private String shopName;
    /** 溯源信息 */
    private String plantingCycle;
    private String originPlaceDetail;
    private String fertilizerType;
    private String storageMethod;
    private String transportMethod;
    private String qrCodeUrl;
}
