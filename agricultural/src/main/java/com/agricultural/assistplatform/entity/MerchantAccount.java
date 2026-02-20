package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchant_account")
public class MerchantAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Integer accountType;
    private String accountNo;
    private String accountName;
    private Integer verifyStatus;
    private Integer auditStatus;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
