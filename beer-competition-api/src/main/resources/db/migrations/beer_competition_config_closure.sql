USE `beer_competition`;

SET @code_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition'
    AND column_name = 'code'
);
SET @sql = IF(@code_column_exists = 0,
  'ALTER TABLE `competition` ADD COLUMN `code` varchar(64) NULL AFTER `id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `competition`
SET `code` = CONCAT('COMP-', `id`)
WHERE `code` IS NULL OR `code` = '';

ALTER TABLE `competition` MODIFY COLUMN `code` varchar(64) NOT NULL;

SET @edition_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition'
    AND column_name = 'edition'
);
SET @sql = IF(@edition_column_exists = 0,
  'ALTER TABLE `competition` ADD COLUMN `edition` varchar(64) NOT NULL DEFAULT '''' AFTER `name`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @registration_start_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition'
    AND column_name = 'registration_start'
);
SET @sql = IF(@registration_start_column_exists = 0,
  'ALTER TABLE `competition` ADD COLUMN `registration_start` datetime DEFAULT NULL AFTER `competition_date`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @competition_code_index_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition'
    AND index_name = 'uk_competition_code'
);
SET @sql = IF(@competition_code_index_exists = 0,
  'ALTER TABLE `competition` ADD UNIQUE KEY `uk_competition_code` (`code`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `competition`
SET `status` = 'PUBLISHED'
WHERE `status` = 'RESULT_PUBLISHED';

CREATE TABLE IF NOT EXISTS `competition_style_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `name` varchar(128) NOT NULL,
  `category_name` varchar(128) DEFAULT NULL,
  `style_code` varchar(64) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_style_name` (`competition_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛基础风格';

CREATE TABLE IF NOT EXISTS `entry_field_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `field_key` varchar(64) NOT NULL,
  `field_label` varchar(64) NOT NULL,
  `field_type` varchar(32) NOT NULL,
  `required_flag` tinyint NOT NULL DEFAULT 0,
  `visible_to_judges` tinyint NOT NULL DEFAULT 0,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_field_config_key` (`competition_id`,`field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名字段配置';

INSERT IGNORE INTO `competition_style_config` (`competition_id`, `name`, `category_name`, `style_code`, `sort_order`)
SELECT `id`, 'American IPA', 'IPA', '21A', 1
FROM `competition`
WHERE `status` <> 'DRAFT';

INSERT IGNORE INTO `entry_field_config` (`competition_id`, `field_key`, `field_label`, `field_type`, `required_flag`, `visible_to_judges`, `sort_order`)
SELECT `id`, 'specialIngredients', '增味原料或特殊工艺', 'textarea', 0, 1, 1
FROM `competition`
WHERE `status` <> 'DRAFT';

INSERT IGNORE INTO `competition_judge_table` (`competition_id`, `table_name`, `sort_order`)
SELECT `id`, 'A桌', 0
FROM `competition`
WHERE `status` <> 'DRAFT';

INSERT INTO `competition_score_config` (`competition_id`, `judge_role_type`, `dimensions_json`)
SELECT `id`, 'CROSS', JSON_ARRAY(
  JSON_OBJECT('key', 'impression', 'label', '整体感受', 'maxScore', 20),
  JSON_OBJECT('key', 'drinkability', 'label', '适饮性', 'maxScore', 20),
  JSON_OBJECT('key', 'story', 'label', '记忆点', 'maxScore', 10)
)
FROM `competition`
WHERE `status` <> 'DRAFT'
ON DUPLICATE KEY UPDATE `dimensions_json` = VALUES(`dimensions_json`);

INSERT IGNORE INTO `competition_score_config` (`competition_id`, `judge_role_type`, `dimensions_json`)
SELECT `id`, 'PROFESSIONAL', JSON_ARRAY(
  JSON_OBJECT('key', 'aroma', 'label', '香气', 'maxScore', 12),
  JSON_OBJECT('key', 'appearance', 'label', '外观', 'maxScore', 3),
  JSON_OBJECT('key', 'flavor', 'label', '味道', 'maxScore', 20),
  JSON_OBJECT('key', 'mouthfeel', 'label', '口感', 'maxScore', 5),
  JSON_OBJECT('key', 'overall', 'label', '整体印象', 'maxScore', 10)
)
FROM `competition`
WHERE `status` <> 'DRAFT';

INSERT IGNORE INTO `competition_score_config` (`competition_id`, `judge_role_type`, `dimensions_json`)
SELECT `id`, 'CAPTAIN', JSON_ARRAY(
  JSON_OBJECT('key', 'consensus', 'label', '共识评分', 'maxScore', 50)
)
FROM `competition`
WHERE `status` <> 'DRAFT';
