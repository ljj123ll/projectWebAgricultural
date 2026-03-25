package com.agricultural.assistplatform.dto.merchant;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantWithdrawApplyDTO {
    private BigDecimal amount;
    private Long accountId;
}

