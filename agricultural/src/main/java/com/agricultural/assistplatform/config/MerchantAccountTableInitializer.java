package com.agricultural.assistplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantAccountTableInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("verify_amount",
                "ALTER TABLE merchant_account ADD COLUMN verify_amount DECIMAL(6,2) NULL COMMENT '1分钱验证金额' AFTER audit_status");
        addColumnIfMissing("verify_expire_time",
                "ALTER TABLE merchant_account ADD COLUMN verify_expire_time DATETIME NULL COMMENT '验证金额过期时间' AFTER verify_amount");
        addColumnIfMissing("verified_time",
                "ALTER TABLE merchant_account ADD COLUMN verified_time DATETIME NULL COMMENT '验证通过时间' AFTER verify_expire_time");
        addColumnIfMissing("audit_submit_time",
                "ALTER TABLE merchant_account ADD COLUMN audit_submit_time DATETIME NULL COMMENT '提交审核时间' AFTER verified_time");
        addColumnIfMissing("reject_reason",
                "ALTER TABLE merchant_account ADD COLUMN reject_reason VARCHAR(255) NULL COMMENT '审核驳回原因' AFTER audit_submit_time");
    }

    private void addColumnIfMissing(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'merchant_account'
              AND COLUMN_NAME = ?
            """, Integer.class, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}

