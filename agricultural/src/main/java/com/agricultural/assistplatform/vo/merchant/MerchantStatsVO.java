package com.agricultural.assistplatform.vo.merchant;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MerchantStatsVO {
    // 近7天数据
    private Integer salesCount7d;
    private BigDecimal revenue7d;
    private List<HotProductVO> hotProducts;
    
    // 今日数据
    private Integer todayOrders;
    private BigDecimal todaySales;
    
    // 昨日数据
    private Integer yesterdayOrders;
    private BigDecimal yesterdaySales;
    
    // 待处理数据
    private Integer pendingOrders;      // 待发货订单数
    private Integer pendingAfterSales;  // 待处理售后数
    private Integer lowStock;           // 库存预警商品数
    
    // 销售趋势（近7天每日数据）
    private List<DailySalesVO> salesTrend;
}
