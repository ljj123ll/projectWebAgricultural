package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_communication")
public class OrderCommunication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    /**
     * 发送者类型：1-用户 2-商家 3-管理员
     */
    private Integer senderType;

    private Long senderId;

    /**
     * 消息类型：1-文本 2-图片 3-视频
     */
    private Integer messageType;

    private String content;

    private String mediaUrl;

    private String mediaName;

    @TableLogic
    private Integer deleteFlag;

    private LocalDateTime createTime;
}
