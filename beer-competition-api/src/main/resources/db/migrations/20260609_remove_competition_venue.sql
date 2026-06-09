SET @drop_column_sql = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `competition` DROP COLUMN `venue_name`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition'
    AND COLUMN_NAME = 'venue_name'
);
PREPARE stmt FROM @drop_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_column_sql = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `competition` DROP COLUMN `venue_address`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition'
    AND COLUMN_NAME = 'venue_address'
);
PREPARE stmt FROM @drop_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_column_sql = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `competition` DROP COLUMN `venue_time_note`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition'
    AND COLUMN_NAME = 'venue_time_note'
);
PREPARE stmt FROM @drop_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_column_sql = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `competition` DROP COLUMN `venue_contact`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition'
    AND COLUMN_NAME = 'venue_contact'
);
PREPARE stmt FROM @drop_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_column_sql = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `competition` DROP COLUMN `venue_map_url`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'competition'
    AND COLUMN_NAME = 'venue_map_url'
);
PREPARE stmt FROM @drop_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
