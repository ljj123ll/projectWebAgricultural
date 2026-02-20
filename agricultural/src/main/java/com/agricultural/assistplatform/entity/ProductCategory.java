package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_category")
public class ProductCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String categoryName;
    private Long parentId;
    private Integer categoryLevel;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
}
