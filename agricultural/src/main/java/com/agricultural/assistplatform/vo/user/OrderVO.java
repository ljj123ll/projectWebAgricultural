package com.agricultural.assistplatform.vo.user;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private String cancelReason;
    private LocalDateTime payDeadline;
    private String receiver;
    private String receiverPhone;
    private String receiverAddress;
    private Long merchantId;
    private LocalDateTime createTime;
    private String logisticsCompany;
    private String logisticsNo;
    private List<OrderItemVO> items;
}
