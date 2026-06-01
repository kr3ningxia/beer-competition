USE `beer_competition`;

SET @style_library_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition'
    AND column_name = 'style_library_version'
);
SET @sql = IF(@style_library_column_exists = 0,
  'ALTER TABLE `competition` ADD COLUMN `style_library_version` varchar(64) NOT NULL DEFAULT ''BJCP_2021_CN'' AFTER `entry_fee`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @help_text_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'entry_field_config'
    AND column_name = 'help_text'
);
SET @sql = IF(@help_text_column_exists = 0,
  'ALTER TABLE `entry_field_config` ADD COLUMN `help_text` varchar(255) NULL AFTER `field_type`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @options_json_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'entry_field_config'
    AND column_name = 'options_json'
);
SET @sql = IF(@options_json_column_exists = 0,
  'ALTER TABLE `entry_field_config` ADD COLUMN `options_json` json NULL AFTER `help_text`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @min_comment_length_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_score_config'
    AND column_name = 'min_comment_length'
);
SET @sql = IF(@min_comment_length_column_exists = 0,
  'ALTER TABLE `competition_score_config` ADD COLUMN `min_comment_length` int NOT NULL DEFAULT 0 AFTER `judge_role_type`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
