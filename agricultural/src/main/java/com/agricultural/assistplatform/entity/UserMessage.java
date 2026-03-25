package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户消息
 * senderType: 1-系统 2-商家 3-管理员
 */
@Data
@TableName("user_message")
public class UserMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer senderType;
    private String title;
    private String content;
    private String refType;
    private String refNo;
    private Integer isRead;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
