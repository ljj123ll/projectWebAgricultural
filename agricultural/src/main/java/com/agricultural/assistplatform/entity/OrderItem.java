package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long productId;
    private String productName;
    private String productImg;
    private BigDecimal productPrice;
    private Integer productNum;
    private BigDecimal productAmount;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
}
