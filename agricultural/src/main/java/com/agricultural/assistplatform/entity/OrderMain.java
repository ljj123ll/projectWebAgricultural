package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表
 * 订单状态 1-待付款 2-待发货 3-待收货 4-已完成 5-已取消 6-支付异常
 */
@Data
@TableName("order_main")
public class OrderMain {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long addressId;
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private String cancelReason;
    private LocalDateTime payDeadline;
    private String receiver;
    private String receiverPhone;
    private String receiverAddress;
    private Long merchantId;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
