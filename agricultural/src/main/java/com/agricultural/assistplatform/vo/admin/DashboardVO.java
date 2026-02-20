package com.agricultural.assistplatform.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardVO {
    private Long totalOrderCount;
    private BigDecimal totalRevenue;
    private List<MerchantRankVO> merchantRank;
}
