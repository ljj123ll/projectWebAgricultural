package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shop_info")
public class ShopInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String shopName;
    private String shopIntro;
    private String qualificationImg;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
