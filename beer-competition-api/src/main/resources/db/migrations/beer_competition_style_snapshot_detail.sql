USE `beer_competition`;

SET @category_name_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_style_config'
    AND column_name = 'category_name'
);
SET @sql = IF(@category_name_column_exists = 0,
  'ALTER TABLE `competition_style_config` ADD COLUMN `category_name` varchar(128) DEFAULT NULL AFTER `name`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @style_code_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_style_config'
    AND column_name = 'style_code'
);
SET @sql = IF(@style_code_column_exists = 0,
  'ALTER TABLE `competition_style_config` ADD COLUMN `style_code` varchar(64) DEFAULT NULL AFTER `category_name`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @description_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_style_config'
    AND column_name = 'description'
);
SET @sql = IF(@description_column_exists = 0,
  'ALTER TABLE `competition_style_config` ADD COLUMN `description` varchar(500) DEFAULT NULL AFTER `style_code`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `competition_style_config` competition_style
JOIN `competition` competition ON competition.`id` = competition_style.`competition_id`
JOIN `style_library` library ON library.`code` = competition.`style_library_version`
JOIN `style_item` style_item ON style_item.`library_id` = library.`id`
  AND style_item.`name` = competition_style.`name`
LEFT JOIN `style_category` category ON category.`id` = style_item.`category_id`
SET competition_style.`category_name` = category.`name`,
    competition_style.`style_code` = style_item.`style_code`,
    competition_style.`description` = style_item.`description`
WHERE competition_style.`category_name` IS NULL
   OR competition_style.`style_code` IS NULL
   OR competition_style.`description` IS NULL;
