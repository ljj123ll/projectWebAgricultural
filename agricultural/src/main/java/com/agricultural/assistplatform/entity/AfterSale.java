package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后状态 1-待商家处理 2-协商中 3-已解决 4-管理员介入 5-已驳回
 */
@Data
@TableName("after_sale")
public class AfterSale {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String afterSaleNo;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private Integer afterSaleType;
    private String applyReason;
    private String proofImgUrls;
    private Integer afterSaleStatus;
    private String handleResult;
    private Long adminId;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
