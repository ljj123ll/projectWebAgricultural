package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logistics_info")
public class LogisticsInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String logisticsCompany;
    private String logisticsNo;
    private Integer logisticsStatus;
    private String abnormalReason;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
