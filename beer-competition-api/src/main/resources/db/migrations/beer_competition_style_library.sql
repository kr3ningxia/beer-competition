USE `beer_competition`;

CREATE TABLE IF NOT EXISTS `style_library` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `version` varchar(64) NOT NULL,
  `language` varchar(32) NOT NULL,
  `source` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `tags_json` json DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_style_library_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局风格库';

CREATE TABLE IF NOT EXISTS `style_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `library_id` bigint NOT NULL,
  `name` varchar(128) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_style_category_name` (`library_id`,`name`),
  KEY `idx_style_category_library` (`library_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风格库分类';

CREATE TABLE IF NOT EXISTS `style_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `library_id` bigint NOT NULL,
  `category_id` bigint DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `style_code` varchar(64) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_style_item_name` (`library_id`,`name`),
  KEY `idx_style_item_library` (`library_id`,`sort_order`),
  KEY `idx_style_item_category` (`category_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风格库条目';

INSERT INTO `style_library` (`code`, `name`, `version`, `language`, `source`, `status`, `tags_json`)
VALUES
  ('BJCP_2021_CN', 'BJCP 2021 中文标准库', '2021', '中文', 'BJCP', 1, JSON_ARRAY('报名必填', '支持搜索', '评审可见')),
  ('CUSTOM_STANDARD', '主办方标准风格库', '2026A', '中文', '主办方', 1, JSON_ARRAY('自定义分类', '报名可搜', '评审可见')),
  ('BJCP_2021_EN', 'BJCP 2021 English', '2021', 'English', 'BJCP', 0, JSON_ARRAY('英文展示', '支持搜索', '评审可见'))
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `version` = VALUES(`version`),
  `language` = VALUES(`language`),
  `source` = VALUES(`source`),
  `status` = VALUES(`status`),
  `tags_json` = VALUES(`tags_json`);

INSERT INTO `style_category` (`library_id`, `name`, `sort_order`)
SELECT library.id, category.name, category.sort_order
FROM `style_library` library
JOIN (
  SELECT 'BJCP_2021_CN' code, 'IPA' name, 1 sort_order UNION ALL
  SELECT 'BJCP_2021_CN', '拉格', 2 UNION ALL
  SELECT 'BJCP_2021_CN', '世涛与酸啤', 3 UNION ALL
  SELECT 'CUSTOM_STANDARD', '拉格专题', 1 UNION ALL
  SELECT 'CUSTOM_STANDARD', '特色增味', 2 UNION ALL
  SELECT 'BJCP_2021_EN', 'IPA', 1 UNION ALL
  SELECT 'BJCP_2021_EN', 'Lager', 2
) category ON category.code = library.code
ON DUPLICATE KEY UPDATE `sort_order` = VALUES(`sort_order`);

INSERT INTO `style_item` (`library_id`, `category_id`, `name`, `style_code`, `status`, `sort_order`)
SELECT library.id, category.id, item.name, item.style_code, item.status, item.sort_order
FROM `style_library` library
JOIN (
  SELECT 'BJCP_2021_CN' code, 'IPA' category_name, 'American IPA' name, '21A' style_code, 1 status, 1 sort_order UNION ALL
  SELECT 'BJCP_2021_CN', 'IPA', 'Double IPA', '22A', 1, 2 UNION ALL
  SELECT 'BJCP_2021_CN', '拉格', 'Pilsner', '5D', 1, 3 UNION ALL
  SELECT 'BJCP_2021_CN', '世涛与酸啤', 'Imperial Stout', '20C', 1, 4 UNION ALL
  SELECT 'BJCP_2021_CN', '世涛与酸啤', 'Mixed Fermentation Sour', '28B', 1, 5 UNION ALL
  SELECT 'BJCP_2021_CN', '世涛与酸啤', 'Gose', '27A', 1, 6 UNION ALL
  SELECT 'BJCP_2021_CN', '拉格', 'Helles', '4A', 1, 7 UNION ALL
  SELECT 'BJCP_2021_CN', 'IPA', 'Saison', '25B', 1, 8 UNION ALL
  SELECT 'CUSTOM_STANDARD', '拉格专题', '浅色拉格', NULL, 1, 1 UNION ALL
  SELECT 'CUSTOM_STANDARD', '拉格专题', '深色拉格', NULL, 1, 2 UNION ALL
  SELECT 'CUSTOM_STANDARD', '拉格专题', '创意拉格', NULL, 1, 3 UNION ALL
  SELECT 'CUSTOM_STANDARD', '特色增味', '水果酸啤', NULL, 1, 4 UNION ALL
  SELECT 'CUSTOM_STANDARD', '特色增味', '桶陈世涛', NULL, 1, 5 UNION ALL
  SELECT 'CUSTOM_STANDARD', '特色增味', '茶咖啡增味', NULL, 1, 6 UNION ALL
  SELECT 'CUSTOM_STANDARD', '特色增味', '实验啤酒', NULL, 1, 7 UNION ALL
  SELECT 'BJCP_2021_EN', 'IPA', 'American IPA', '21A', 1, 1 UNION ALL
  SELECT 'BJCP_2021_EN', 'IPA', 'Double IPA', '22A', 1, 2 UNION ALL
  SELECT 'BJCP_2021_EN', 'Lager', 'German Pils', '5D', 1, 3 UNION ALL
  SELECT 'BJCP_2021_EN', 'IPA', 'Imperial Stout', '20C', 1, 4 UNION ALL
  SELECT 'BJCP_2021_EN', 'IPA', 'Mixed-Fermentation Sour Beer', '28B', 1, 5 UNION ALL
  SELECT 'BJCP_2021_EN', 'IPA', 'Gose', '27A', 1, 6
) item ON item.code = library.code
LEFT JOIN `style_category` category ON category.library_id = library.id AND category.name = item.category_name
ON DUPLICATE KEY UPDATE
  `category_id` = VALUES(`category_id`),
  `style_code` = VALUES(`style_code`),
  `status` = VALUES(`status`),
  `sort_order` = VALUES(`sort_order`);
