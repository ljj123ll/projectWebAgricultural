package com.agricultural.assistplatform.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RiskOrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private String receiver;
    private String receiverPhone;
    private String receiverAddress;
    private String riskReason;
    private Integer riskScore;
    private LocalDateTime createTime;
}

