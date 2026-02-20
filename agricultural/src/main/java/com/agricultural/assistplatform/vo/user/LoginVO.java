package com.agricultural.assistplatform.vo.user;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private UserInfoVO userInfo;
}
