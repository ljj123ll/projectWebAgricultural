package com.agricultural.assistplatform.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardVO {
    // 核心统计数据
    private Long totalUserCount;      // 总用户数
    private Long totalMerchantCount;  // 总商家数
    private Long totalOrderCount;     // 总订单数
    private BigDecimal totalRevenue;  // 总交易额
    private BigDecimal todayRevenue;  // 今日交易额
    private Long pendingAuditCount;   // 待审核数量
    
    // 商家销量排行
    private List<MerchantRankVO> merchantRank;
    
    // 最近订单数据
    private List<RecentOrderVO> recentOrders;
    
    // 系统公告
    private List<SystemNoticeVO> notices;
}
