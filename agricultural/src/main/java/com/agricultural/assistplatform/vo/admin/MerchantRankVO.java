package com.agricultural.assistplatform.vo.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantRankVO {
    private Long merchantId;
    private String merchantName;
    private Integer salesVolume;
    private BigDecimal revenue;
}
