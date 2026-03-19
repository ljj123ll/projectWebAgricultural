package com.agricultural.assistplatform.vo.admin;

import lombok.Data;

@Data
public class AdminMerchantDetailVO {
    private Long id;
    private String phone;
    private String merchantName;
    private String contactPerson;
    private String contactPhone;
    private Integer auditStatus;
    private String rejectReason;
    private Integer status;

    private String idCard;
    private String idCardFront;
    private String idCardBack;
    private String licenseImg;

    private Long shopId;
    private String shopName;
    private String shopIntro;
    private String qualificationImg;
    private String shopType;
    private String categories;
}
