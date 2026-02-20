package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("subsidy_detail")
public class SubsidyDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String orderNo;
    private String subsidyType;
    private BigDecimal subsidyAmount;
    private Integer auditStatus;
    private Integer grantStatus;
    private LocalDateTime grantTime;
    private String rejectReason;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
