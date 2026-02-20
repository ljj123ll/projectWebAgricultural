package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("after_sale_communication")
public class AfterSaleCommunication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String afterSaleNo;
    private Integer senderType;
    private Long senderId;
    private String content;
    private Integer isRead;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
}
