package com.agricultural.assistplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐订单备注字段，避免历史库缺列导致备注无法保存/展示。
 */
@Component
@RequiredArgsConstructor
public class OrderMainTableInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'order_main'
              AND COLUMN_NAME = 'remark'
            """, Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute("""
                ALTER TABLE order_main
                ADD COLUMN remark VARCHAR(255) NULL COMMENT '订单备注' AFTER receiver_address
                """);
        }
    }
}
