package com.agricultural.assistplatform.vo.user;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TraceArchiveVO {
    private Long productId;
    private String traceCode;
    private String batchNo;
    private Long categoryId;
    private String categoryName;
    private String productName;
    private String productImg;
    private String productDesc;
    private String shopName;
    private String originPlace;
    private String originPlaceDetail;
    private String plantingCycle;
    private String fertilizerType;
    private String storageMethod;
    private String transportMethod;
    private String inspectionReport;
    private LocalDate productionDate;
    private LocalDate harvestDate;
    private LocalDate packagingDate;
    private List<TraceExtraFieldVO> baseFields;
    private List<TraceExtraFieldVO> featureFields;
}
