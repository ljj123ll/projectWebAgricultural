package com.agricultural.assistplatform.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecentOrderVO {
    private Long orderId;
    private String orderNo;
    private String merchantName;
    private BigDecimal orderAmount;
    private Integer orderStatus;
    private LocalDateTime createTime;
}
