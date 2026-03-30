package com.agricultural.assistplatform.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AdminProductAuditVO {
    private Long id;
    private String productName;
    private Long merchantId;
    private String merchantName;
    private String shopName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private Integer stock;
    private String productImg;
    private String productDetailImg;
    private String productDesc;
    private String originPlace;
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
    private Integer status;
    private String rejectReason;
    private LocalDateTime createTime;
}
