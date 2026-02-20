package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("news_category")
public class NewsCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String categoryName;
    private String categoryCode;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
}
