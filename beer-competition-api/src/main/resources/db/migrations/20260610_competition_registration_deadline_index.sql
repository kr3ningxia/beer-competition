SET @index_exists := (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'competition'
      AND INDEX_NAME = 'idx_competition_status_registration_deadline'
);

SET @ddl := IF(@index_exists = 0,
    'ALTER TABLE `competition` ADD INDEX `idx_competition_status_registration_deadline` (`status`, `registration_deadline`)',
    'SELECT ''idx_competition_status_registration_deadline already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
