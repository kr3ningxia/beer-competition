USE `beer_competition`;

SET @deleted_flag_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition'
    AND column_name = 'deleted_flag'
);
SET @sql = IF(@deleted_flag_column_exists = 0,
  'ALTER TABLE `competition` ADD COLUMN `deleted_flag` tinyint NOT NULL DEFAULT 0 AFTER `style_library_version`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `competition`
SET `deleted_flag` = 0
WHERE `deleted_flag` IS NULL;

SET @competition_code_deleted_index_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition'
    AND index_name = 'uk_competition_code_deleted'
);
SET @competition_code_index_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition'
    AND index_name = 'uk_competition_code'
);
SET @sql = IF(@competition_code_index_exists > 0,
  'SELECT 1',
  IF(@competition_code_deleted_index_exists > 0,
    'ALTER TABLE `competition` DROP INDEX `uk_competition_code_deleted`, ADD UNIQUE KEY `uk_competition_code` (`code`)',
    'ALTER TABLE `competition` ADD UNIQUE KEY `uk_competition_code` (`code`)'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @competition_code_deleted_index_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition'
    AND index_name = 'uk_competition_code_deleted'
);
SET @sql = IF(@competition_code_deleted_index_exists > 0,
  'ALTER TABLE `competition` DROP INDEX `uk_competition_code_deleted`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
