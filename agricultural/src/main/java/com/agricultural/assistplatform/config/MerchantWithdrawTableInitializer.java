package com.agricultural.assistplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantWithdrawTableInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS merchant_withdraw (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                    withdraw_no VARCHAR(40) NOT NULL COMMENT '提现单号',
                    merchant_id BIGINT NOT NULL COMMENT '商家ID',
                    account_id BIGINT NOT NULL COMMENT '收款账户ID',
                    account_type TINYINT NOT NULL COMMENT '账户类型 1-银行卡 2-支付宝 3-微信收款',
                    account_no VARCHAR(64) NOT NULL COMMENT '收款账号快照',
                    account_name VARCHAR(64) NOT NULL COMMENT '收款账户名快照',
                    apply_amount DECIMAL(12,2) NOT NULL COMMENT '申请金额',
                    fee_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '提现手续费',
                    actual_amount DECIMAL(12,2) NOT NULL COMMENT '实际打款金额',
                    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待审核 1-待打款 2-已驳回 3-打款成功 4-打款失败待重试 5-打款失败人工处理 6-已取消',
                    audit_admin_id BIGINT NULL COMMENT '审核管理员ID',
                    audit_remark VARCHAR(255) NULL COMMENT '审核备注',
                    audit_time DATETIME NULL COMMENT '审核时间',
                    transfer_no VARCHAR(64) NULL COMMENT '打款流水号',
                    transfer_time DATETIME NULL COMMENT '打款时间',
                    retry_count TINYINT NOT NULL DEFAULT 0 COMMENT '失败重试次数',
                    fail_reason VARCHAR(255) NULL COMMENT '打款失败原因',
                    delete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_withdraw_no (withdraw_no),
                    INDEX idx_merchant_status (merchant_id, status),
                    INDEX idx_status_retry (status, retry_count),
                    INDEX idx_create_time (create_time)
                ) COMMENT='商家提现申请表'
                """);

        addColumnIfMissing("fee_amount",
                "ALTER TABLE merchant_withdraw ADD COLUMN fee_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '提现手续费' AFTER apply_amount");
        addColumnIfMissing("actual_amount",
                "ALTER TABLE merchant_withdraw ADD COLUMN actual_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '实际打款金额' AFTER fee_amount");
        addColumnIfMissing("retry_count",
                "ALTER TABLE merchant_withdraw ADD COLUMN retry_count TINYINT NOT NULL DEFAULT 0 COMMENT '失败重试次数' AFTER transfer_time");
        addColumnIfMissing("fail_reason",
                "ALTER TABLE merchant_withdraw ADD COLUMN fail_reason VARCHAR(255) NULL COMMENT '打款失败原因' AFTER retry_count");
    }

    private void addColumnIfMissing(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'merchant_withdraw'
              AND COLUMN_NAME = ?
            """, Integer.class, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}

