package com.agricultural.assistplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_record")
public class AuditRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String businessType;
    private Long businessId;
    private Long auditUserId;
    private Integer auditStatus;
    private String auditReason;
    @TableLogic
    private Integer deleteFlag;
    private LocalDateTime createTime;
}
