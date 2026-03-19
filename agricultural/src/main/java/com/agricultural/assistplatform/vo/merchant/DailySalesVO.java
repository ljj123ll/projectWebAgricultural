package com.agricultural.assistplatform.vo.merchant;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DailySalesVO {
    private String date;           // 日期，格式：MM-dd
    private Integer orderCount;    // 订单数
    private BigDecimal sales;      // 销售额
}
