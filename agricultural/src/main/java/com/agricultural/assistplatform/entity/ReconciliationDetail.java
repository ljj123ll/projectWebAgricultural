package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("reconciliation_detail")
public class ReconciliationDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String orderNo;
    private BigDecimal orderAmount;
    private BigDecimal actualIncome;
    private BigDecimal serviceFee;
    private LocalDateTime paymentTime;
    private Integer transferStatus;
    private LocalDateTime transferTime;
    private String transferNo;
    private Integer retryCount;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
