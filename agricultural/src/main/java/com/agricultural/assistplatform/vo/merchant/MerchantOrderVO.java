package com.agricultural.assistplatform.vo.merchant;

import com.agricultural.assistplatform.vo.user.OrderItemVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MerchantOrderVO {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private String receiver;
    private String receiverPhone;
    private String receiverAddress;
    private LocalDateTime createTime;
    private String logisticsCompany;
    private String logisticsNo;
    private List<OrderItemVO> items;
}
