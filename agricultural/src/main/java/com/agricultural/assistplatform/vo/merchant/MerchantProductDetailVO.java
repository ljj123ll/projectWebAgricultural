package com.agricultural.assistplatform.vo.merchant;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantProductDetailVO {
    private Long id;
    private String productName;
    private Long categoryId;
    private BigDecimal price;
    private Integer stock;
    private Integer stockWarning;
    private String productImg;
    private String productDetailImg;
    private String productDesc;
    private String originPlace;
    private Integer status;
    private String plantingCycle;
    private String originPlaceDetail;
    private String fertilizerType;
    private String storageMethod;
    private String transportMethod;
    private String qrCodeUrl;
}
