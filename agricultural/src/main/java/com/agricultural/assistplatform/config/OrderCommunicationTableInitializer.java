package com.agricultural.assistplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时确保订单沟通表存在（用户/商家/管理员订单实时沟通）。
 */
@Component
@RequiredArgsConstructor
public class OrderCommunicationTableInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS order_communication (
                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
                order_no VARCHAR(64) NOT NULL COMMENT '订单号',
                sender_type TINYINT NOT NULL COMMENT '发送者类型 1-用户 2-商家 3-管理员',
                sender_id BIGINT NOT NULL COMMENT '发送者ID',
                message_type TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型 1-文本 2-图片 3-视频',
                content VARCHAR(1000) NOT NULL COMMENT '消息内容',
                media_url VARCHAR(255) NULL COMMENT '媒体地址',
                media_name VARCHAR(255) NULL COMMENT '媒体原始名称',
                delete_flag TINYINT DEFAULT 0 COMMENT '软删除 0-未删 1-已删',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                INDEX idx_order_no (order_no),
                INDEX idx_order_time (order_no, create_time),
                INDEX idx_sender (sender_type, sender_id)
            ) COMMENT = '订单沟通消息表'
            """);

        ensureColumn(
                "message_type",
                "ALTER TABLE order_communication ADD COLUMN message_type TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型 1-文本 2-图片 3-视频' AFTER sender_id"
        );
        ensureColumn(
                "media_url",
                "ALTER TABLE order_communication ADD COLUMN media_url VARCHAR(255) NULL COMMENT '媒体地址' AFTER content"
        );
        ensureColumn(
                "media_name",
                "ALTER TABLE order_communication ADD COLUMN media_name VARCHAR(255) NULL COMMENT '媒体原始名称' AFTER media_url"
        );
    }

    private void ensureColumn(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'order_communication'
              AND COLUMN_NAME = ?
            """, Integer.class, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }
}
