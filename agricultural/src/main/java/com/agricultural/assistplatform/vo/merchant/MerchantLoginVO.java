package com.agricultural.assistplatform.vo.merchant;

import lombok.Data;

@Data
public class MerchantLoginVO {
    private String token;
    private Long merchantId;
    private String merchantName;
    private Integer auditStatus;
    private Integer status;
}
