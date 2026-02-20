package com.agricultural.assistplatform.dto.merchant;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品新增/编辑：基础信息 + 溯源信息
 */
@Data
public class ProductSaveDTO {
    private String productName;
    private Long categoryId;
    private BigDecimal price;
    private Integer stock;
    private Integer stockWarning;
    private String productImg;
    private String productDesc;
    private String originPlace;
    private String plantingCycle;
    private String originPlaceDetail;
    private String fertilizerType;
    private String storageMethod;
    private String transportMethod;
}
