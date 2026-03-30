package com.agricultural.assistplatform.dto.merchant;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

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
    private String productDetailImg; // 详细介绍图片，逗号分隔多图
    private String productDesc;
    private String originPlace;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate harvestDate;
    private LocalDate packagingDate;
    private String inspectionReport;
    private String plantingCycle;
    private String originPlaceDetail;
    private String fertilizerType;
    private String storageMethod;
    private String transportMethod;
    private Map<String, String> traceExtra;
}
