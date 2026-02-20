package com.agricultural.assistplatform.dto.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MerchantRegisterDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "1[3-9]\\d{9}", message = "手机号格式错误")
    private String phone;
    @NotBlank(message = "验证码不能为空")
    private String code;
    @NotBlank(message = "农户/合作社名称不能为空")
    private String merchantName;
    @NotBlank(message = "联系人不能为空")
    private String contactPerson;
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;
    private String password;
}
