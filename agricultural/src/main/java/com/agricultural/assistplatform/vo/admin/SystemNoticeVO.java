package com.agricultural.assistplatform.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemNoticeVO {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createTime;
}
