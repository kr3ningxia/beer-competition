SET @drop_column_sql = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `beer_entry` DROP COLUMN `description`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'beer_entry'
    AND COLUMN_NAME = 'description'
);
PREPARE stmt FROM @drop_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
