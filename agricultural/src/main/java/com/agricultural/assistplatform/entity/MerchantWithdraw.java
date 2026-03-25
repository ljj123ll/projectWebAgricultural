package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant_withdraw")
public class MerchantWithdraw {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String withdrawNo;
    private Long merchantId;
    private Long accountId;
    private Integer accountType;
    private String accountNo;
    private String accountName;
    private BigDecimal applyAmount;
    private BigDecimal feeAmount;
    private BigDecimal actualAmount;
    /**
     * 状态: 0-待审核 1-待打款 2-已驳回 3-打款成功 4-打款失败待重试 5-打款失败人工处理 6-已取消
     */
    private Integer status;
    private Long auditAdminId;
    private String auditRemark;
    private LocalDateTime auditTime;
    private String transferNo;
    private LocalDateTime transferTime;
    private Integer retryCount;
    private String failReason;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

