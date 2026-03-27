package com.agricultural.assistplatform.vo.common;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UnsalableProductVO {
    private Long id;
    private Long merchantId;
    private String productName;
    private BigDecimal price;
    private Integer salesVolume;
    private Integer stock;
    private String productImg;
    private String originPlace;
    private Long categoryId;
    private String categoryName;
    private String merchantName;
    private LocalDateTime createTime;
    private Boolean manualIncluded;
    private Boolean algorithmIncluded;
    private String inclusionSource;
    private Integer unsalableScore;
    private Long ageDays;
    private String unsalableReason;
}
