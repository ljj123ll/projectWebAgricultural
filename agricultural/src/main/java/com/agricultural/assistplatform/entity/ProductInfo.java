package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品状态 0-待审核 1-已上架 2-已下架 3-已驳回
 */
@Data
@TableName("product_info")
public class ProductInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productName;
    private Long merchantId;
    private Long categoryId;
    private BigDecimal price;
    private Integer stock;
    private Integer stockWarning;
    private String productImg;
    private String productDesc;
    private String originPlace;
    private Integer status;
    private String rejectReason;
    private Integer isUnsalable;
    private Integer salesVolume;
    private BigDecimal score;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
