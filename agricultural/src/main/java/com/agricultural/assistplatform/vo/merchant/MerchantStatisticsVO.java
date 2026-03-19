package com.agricultural.assistplatform.vo.merchant;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MerchantStatisticsVO {
    // 数据概览
    private BigDecimal sales;
    private Integer orders;
    private Integer visitors;
    private BigDecimal conversionRate;
    
    // 趋势（与上期对比）
    private BigDecimal salesTrend;
    private BigDecimal ordersTrend;
    private BigDecimal visitorsTrend;
    private BigDecimal conversionTrend;
    
    // 销售趋势（图表数据）
    private List<DailySalesVO> salesTrendData;
    
    // 热销商品TOP5
    private List<HotProductVO> topProducts;
    
    // 订单状态分布
    private List<OrderStatusStatVO> orderStatusDistribution;
}
