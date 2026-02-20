package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消费者用户表
 */
@Data
@TableName("user_info")
public class UserInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String nickname;
    private String password;
    private String avatarUrl;
    private LocalDateTime lastLoginTime;
    private String loginToken;
    private LocalDateTime tokenExpireTime;
    private Integer status;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
