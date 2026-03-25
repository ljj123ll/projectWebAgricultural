package com.agricultural.assistplatform.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AfterSaleReturnLogisticsDTO {
    @NotBlank(message = "退货物流单号不能为空")
    private String logisticsNo;
    private String logisticsCompany;
}
