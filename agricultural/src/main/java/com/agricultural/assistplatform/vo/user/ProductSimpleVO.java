package com.agricultural.assistplatform.vo.user;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSimpleVO {
    private Long id;
    private String productName;
    private BigDecimal price;
    private Integer salesVolume;
    private Integer stock;
    private String productImg;
    private BigDecimal score;
}
