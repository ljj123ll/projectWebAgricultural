package com.agricultural.assistplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐售后扩展字段，避免历史库缺列导致新流程不可用。
 */
@Component
@RequiredArgsConstructor
public class AfterSaleTableInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("origin_order_status",
                "ALTER TABLE after_sale ADD COLUMN origin_order_status TINYINT NULL COMMENT '申请售后前订单状态' AFTER after_sale_status");
        addColumnIfMissing("return_logistics_company",
                "ALTER TABLE after_sale ADD COLUMN return_logistics_company VARCHAR(50) NULL COMMENT '退货物流公司' AFTER origin_order_status");
        addColumnIfMissing("return_logistics_no",
                "ALTER TABLE after_sale ADD COLUMN return_logistics_no VARCHAR(64) NULL COMMENT '退货物流单号' AFTER return_logistics_company");
        addColumnIfMissing("return_ship_time",
                "ALTER TABLE after_sale ADD COLUMN return_ship_time DATETIME NULL COMMENT '用户退货时间' AFTER return_logistics_no");
    }

    private void addColumnIfMissing(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'after_sale'
              AND COLUMN_NAME = ?
            """, Integer.class, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
