package com.agricultural.assistplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductTraceTableInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("trace_code", "ALTER TABLE product_trace ADD COLUMN trace_code VARCHAR(64) NULL COMMENT '溯源码' AFTER product_id");
        addColumnIfMissing("batch_no", "ALTER TABLE product_trace ADD COLUMN batch_no VARCHAR(64) NULL COMMENT '批次编号' AFTER trace_code");
        addColumnIfMissing("production_date", "ALTER TABLE product_trace ADD COLUMN production_date DATE NULL COMMENT '生产建档日期' AFTER batch_no");
        addColumnIfMissing("harvest_date", "ALTER TABLE product_trace ADD COLUMN harvest_date DATE NULL COMMENT '采收/出栏日期' AFTER production_date");
        addColumnIfMissing("packaging_date", "ALTER TABLE product_trace ADD COLUMN packaging_date DATE NULL COMMENT '包装入库日期' AFTER harvest_date");
        addColumnIfMissing("inspection_report", "ALTER TABLE product_trace ADD COLUMN inspection_report VARCHAR(500) NULL COMMENT '检测/检疫说明' AFTER packaging_date");
        addColumnIfMissing("trace_extra", "ALTER TABLE product_trace ADD COLUMN trace_extra TEXT NULL COMMENT '分类溯源扩展字段(JSON)' AFTER inspection_report");
        expandQrCodeColumnIfNeeded();
    }

    private void addColumnIfMissing(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'product_trace'
              AND COLUMN_NAME = ?
            """, Integer.class, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void expandQrCodeColumnIfNeeded() {
        String dataType = jdbcTemplate.queryForObject("""
            SELECT DATA_TYPE
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'product_trace'
              AND COLUMN_NAME = 'qr_code_url'
            """, String.class);
        if (dataType != null && !"text".equalsIgnoreCase(dataType) && !"mediumtext".equalsIgnoreCase(dataType)
                && !"longtext".equalsIgnoreCase(dataType)) {
            jdbcTemplate.execute("ALTER TABLE product_trace MODIFY COLUMN qr_code_url TEXT NULL COMMENT '溯源二维码地址或DataURL'");
        }
    }
}
