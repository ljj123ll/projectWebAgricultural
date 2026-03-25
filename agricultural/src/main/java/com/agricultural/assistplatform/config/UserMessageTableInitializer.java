package com.agricultural.assistplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时确保用户消息表存在，避免新功能上线后手动建表遗漏。
 */
@Component
@RequiredArgsConstructor
public class UserMessageTableInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS user_message (
                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
                user_id BIGINT NOT NULL COMMENT '用户ID',
                sender_type TINYINT NOT NULL COMMENT '发送方类型 1-系统 2-商家 3-管理员',
                title VARCHAR(100) COMMENT '消息标题',
                content TEXT NOT NULL COMMENT '消息内容',
                ref_type VARCHAR(30) COMMENT '关联类型（order/after_sale/comment/product）',
                ref_no VARCHAR(64) COMMENT '关联编号',
                is_read TINYINT DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
                delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                INDEX idx_user_id (user_id),
                INDEX idx_user_read (user_id, is_read),
                INDEX idx_sender_type (sender_type)
            ) COMMENT = '用户消息表'
            """);
    }
}
