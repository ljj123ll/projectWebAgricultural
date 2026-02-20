package com.agricultural.assistplatform.vo.user;

import lombok.Data;

@Data
public class UserAddressVO {
    private Long id;
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String county;
    private String town;
    private String detailAddress;
    private Integer isDefault;
}
