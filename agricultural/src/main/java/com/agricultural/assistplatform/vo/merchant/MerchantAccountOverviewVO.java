package com.agricultural.assistplatform.vo.merchant;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantAccountOverviewVO {
    private BigDecimal balance;
    private BigDecimal totalIncome;
    private BigDecimal totalServiceFee;
    private BigDecimal todayIncome;
    private BigDecimal weekIncome;
    private BigDecimal monthIncome;
    private Integer pendingTransferCount;
    private Integer failedTransferCount;
    private Integer manualFallbackCount;
    private BigDecimal subsidyTotal;
    private BigDecimal subsidyMonth;
    private Integer subsidyOrderCount;
    private Integer subsidyPendingCount;
    private Integer subsidyRejectedCount;
    private Boolean hasPassedReceivingAccount;
    private Integer approvedAccountType;
    private String approvedAccountName;
    private String approvedAccountNoMask;
    private BigDecimal withdrawAvailableAmount;
    private BigDecimal withdrawFrozenAmount;
    private BigDecimal withdrawSuccessAmount;
}
