package com.agricultural.assistplatform.vo.merchant;

import lombok.Data;

@Data
public class ShopDetailVO {
    private String shopName;
    private String shopIntro;
    private String qualificationImg;
    private String shopType;
    private String categories;
    private String shopAddress;

    private String contactName;
    private String contactPhone;
    private String ownerName;
    private String idCard;
    private String idCardFront;
    private String idCardBack;
    private String licenseImg;

    private Integer auditStatus;
    private String rejectReason;
}
