package com.agricultural.assistplatform.vo.merchant;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MerchantStatsVO {
    private Integer salesCount7d;
    private BigDecimal revenue7d;
    private List<HotProductVO> hotProducts;
}
