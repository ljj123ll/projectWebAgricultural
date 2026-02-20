package com.agricultural.assistplatform.vo.user;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO {
    private Long productId;
    private String productName;
    private String productImg;
    private BigDecimal productPrice;
    private Integer productNum;
    private BigDecimal productAmount;
}
