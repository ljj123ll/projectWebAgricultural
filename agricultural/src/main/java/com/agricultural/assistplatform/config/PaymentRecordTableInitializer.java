package com.agricultural.assistplatform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecordTableInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addUniqueOrderNoIndexIfPossible();
    }

    private void addUniqueOrderNoIndexIfPossible() {
        Integer indexCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'payment_record'
              AND INDEX_NAME = 'uk_order_no'
            """, Integer.class);
        if (indexCount != null && indexCount > 0) {
            return;
        }

        Integer duplicateOrderCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM (
                SELECT order_no
                FROM payment_record
                WHERE delete_flag = 0
                GROUP BY order_no
                HAVING COUNT(*) > 1
            ) t
            """, Integer.class);
        if (duplicateOrderCount != null && duplicateOrderCount > 0) {
            log.warn("payment_record 表存在重复 order_no 数据，已跳过唯一索引初始化，请先清理历史数据");
            return;
        }

        jdbcTemplate.execute("ALTER TABLE payment_record ADD UNIQUE KEY uk_order_no (order_no)");
    }
}
