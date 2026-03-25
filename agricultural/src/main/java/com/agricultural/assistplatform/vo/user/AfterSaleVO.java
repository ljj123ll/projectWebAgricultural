package com.agricultural.assistplatform.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AfterSaleVO {
    private Long id;
    private String afterSaleNo;
    private String orderNo;
    private Integer afterSaleType;
    private String applyReason;
    private String proofImgUrls;
    private Integer afterSaleStatus;
    private Integer originOrderStatus;
    private String returnLogisticsCompany;
    private String returnLogisticsNo;
    private LocalDateTime returnShipTime;
    private String handleResult;
    private LocalDateTime createTime;
}
