package com.agricultural.assistplatform.vo.merchant;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

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
    private String traceCode;
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
    private String qrCodeUrl;
    private Map<String, String> traceExtra;
}
