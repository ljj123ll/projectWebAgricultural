package com.agricultural.assistplatform.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AfterSaleApplyDTO {
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;
    @NotNull(message = "售后类型不能为空")
    private Integer afterSaleType;
    @NotBlank(message = "申请原因不能为空")
    private String applyReason;
    private String proofImgUrls;
}
