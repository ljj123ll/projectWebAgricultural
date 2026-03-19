package com.agricultural.assistplatform.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminProductAuditVO {
    private Long id;
    private String productName;
    private Long merchantId;
    private String merchantName;
    private String shopName;
    private Long categoryId;
    private BigDecimal price;
    private Integer stock;
    private String productImg;
    private String productDesc;
    private Integer status;
    private String rejectReason;
    private LocalDateTime createTime;
}
