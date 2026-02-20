package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_trace")
public class ProductTrace {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String plantingCycle;
    private String originPlaceDetail;
    private String fertilizerType;
    private String storageMethod;
    private String transportMethod;
    private String qrCodeUrl;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
