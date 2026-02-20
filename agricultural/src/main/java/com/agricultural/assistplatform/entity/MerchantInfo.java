package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家信息表
 * audit_status 0-待审核 1-已通过 2-已驳回
 * status 0-未开业 1-正常营业 2-禁用
 */
@Data
@TableName("merchant_info")
public class MerchantInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String merchantName;
    private String contactPerson;
    private String contactPhone;
    private String password;
    private Integer auditStatus;
    private String rejectReason;
    private Integer status;
    private String loginToken;
    private LocalDateTime tokenExpireTime;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
