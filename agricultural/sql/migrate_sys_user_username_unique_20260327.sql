USE nongnong_ecommerce;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND index_name = 'uk_sys_user_username'
);

SET @ddl := IF(
    @idx_exists = 0,
    'ALTER TABLE sys_user ADD UNIQUE KEY uk_sys_user_username (username)',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
