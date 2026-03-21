package com.agricultural.assistplatform.vo.admin;

import com.agricultural.assistplatform.vo.user.OrderItemVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminOrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long addressId;
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private String cancelReason;
    private LocalDateTime payDeadline;
    private LocalDateTime payTime;
    private String receiver;
    private String receiverPhone;
    private String receiverAddress;
    private Long merchantId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String logisticsCompany;
    private String logisticsNo;
    private Integer logisticsStatus;
    private String abnormalReason;
    private List<OrderItemVO> items;
}
