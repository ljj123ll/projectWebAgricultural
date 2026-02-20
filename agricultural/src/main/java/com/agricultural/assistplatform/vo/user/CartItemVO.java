package com.agricultural.assistplatform.vo.user;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImg;
    private BigDecimal price;
    private Integer productNum;
    private Integer stock;
}
