CREATE DATABASE IF NOT EXISTS `beer_competition` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `beer_competition`;

DROP TABLE IF EXISTS `score_record`;
DROP TABLE IF EXISTS `beer_entry_extra_field`;
DROP TABLE IF EXISTS `beer_entry`;
DROP TABLE IF EXISTS `judge_assignment`;
DROP TABLE IF EXISTS `judge_table`;
DROP TABLE IF EXISTS `competition_score_config`;
DROP TABLE IF EXISTS `entry_field_config`;
DROP TABLE IF EXISTS `competition_style_config`;
DROP TABLE IF EXISTS `competition_category`;
DROP TABLE IF EXISTS `competition`;
DROP TABLE IF EXISTS `judge_account`;
DROP TABLE IF EXISTS `portal_account`;
DROP TABLE IF EXISTS `brewery`;
DROP TABLE IF EXISTS `admin_user`;
DROP TABLE IF EXISTS `admin_operation_log`;
DROP TABLE IF EXISTS `sms_code_log`;
DROP TABLE IF EXISTS `file_asset`;

CREATE TABLE `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(64) NOT NULL,
  `name` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员';

CREATE TABLE `brewery` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_name` varchar(128) NOT NULL,
  `contact_name` varchar(64) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `wechat` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂牌';

CREATE TABLE `portal_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `phone` varchar(20) NOT NULL,
  `wechat` varchar(64) DEFAULT NULL,
  `display_name` varchar(64) NOT NULL,
  `brewery_id` bigint NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_portal_account_phone` (`phone`),
  KEY `idx_portal_account_brewery` (`brewery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商账号';

CREATE TABLE `judge_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `public_id` varchar(32) NOT NULL,
  `phone_enc` text NOT NULL,
  `phone_hash` char(64) NOT NULL,
  `phone_last4` varchar(4) DEFAULT NULL,
  `wechat_enc` text DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  `qualification` varchar(255) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `submitted_time` datetime DEFAULT NULL,
  `reviewed_time` datetime DEFAULT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  `review_remark` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_judge_account_public_id` (`public_id`),
  UNIQUE KEY `uk_judge_account_phone_hash` (`phone_hash`),
  KEY `idx_judge_account_phone_last4` (`phone_last4`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评审账号';

CREATE TABLE `competition` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `edition` varchar(64) NOT NULL DEFAULT '',
  `competition_date` date NOT NULL,
  `registration_start` datetime DEFAULT NULL,
  `registration_deadline` datetime NOT NULL,
  `status` varchar(32) NOT NULL,
  `entry_fee` decimal(10,2) NOT NULL DEFAULT 0,
  `style_library_version` varchar(64) NOT NULL DEFAULT 'BJCP_2021_CN',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛';

CREATE TABLE `competition_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `name` varchar(128) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_category_name` (`competition_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛组别';

CREATE TABLE `competition_style_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `name` varchar(128) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_style_name` (`competition_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛基础风格';

CREATE TABLE `entry_field_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `field_key` varchar(64) NOT NULL,
  `field_label` varchar(64) NOT NULL,
  `field_type` varchar(32) NOT NULL,
  `help_text` varchar(255) DEFAULT NULL,
  `options_json` json DEFAULT NULL,
  `required_flag` tinyint NOT NULL DEFAULT 0,
  `visible_to_judges` tinyint NOT NULL DEFAULT 0,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_field_config_key` (`competition_id`,`field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名字段配置';

CREATE TABLE `competition_score_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `judge_role_type` varchar(32) NOT NULL,
  `min_comment_length` int NOT NULL DEFAULT 0,
  `dimensions_json` json NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_score_config` (`competition_id`,`judge_role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分维度配置';

CREATE TABLE `judge_table` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `table_name` varchar(64) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_judge_table_name` (`competition_id`,`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评审桌';

CREATE TABLE `judge_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `judge_account_id` bigint NOT NULL,
  `table_id` bigint NOT NULL,
  `role` varchar(32) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_judge_assignment` (`competition_id`,`judge_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评审分配';

CREATE TABLE `beer_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uuid` varchar(64) NOT NULL,
  `competition_id` bigint NOT NULL,
  `brewery_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `name` varchar(128) NOT NULL,
  `style` varchar(128) NOT NULL,
  `abv` decimal(4,1) NOT NULL,
  `description` varchar(500) NOT NULL,
  `extra_fields_json` json DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_beer_entry_uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参赛酒款';

CREATE TABLE `beer_entry_extra_field` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `beer_entry_id` bigint NOT NULL,
  `field_key` varchar(64) NOT NULL,
  `field_label` varchar(64) NOT NULL,
  `field_value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒款扩展字段';

CREATE TABLE `score_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `beer_entry_id` bigint NOT NULL,
  `judge_account_id` bigint NOT NULL,
  `assignment_id` bigint DEFAULT NULL,
  `judge_role_type` varchar(32) NOT NULL,
  `dimensions_json` json NOT NULL,
  `total_score` decimal(6,2) NOT NULL DEFAULT 0,
  `comments` varchar(1000) NOT NULL,
  `is_final` tinyint NOT NULL DEFAULT 0,
  `is_advanced` tinyint NOT NULL DEFAULT 0,
  `consensus_score` decimal(6,2) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分记录';

CREATE TABLE `sms_code_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `phone_hash` char(64) NOT NULL,
  `masked_phone` varchar(20) NOT NULL,
  `biz_type` varchar(32) NOT NULL,
  `status` varchar(16) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sms_code_log_phone_hash` (`phone_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信验证码日志';

CREATE TABLE `admin_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint NOT NULL,
  `action` varchar(64) NOT NULL,
  `target_type` varchar(32) NOT NULL,
  `target_public_id` varchar(64) DEFAULT NULL,
  `summary` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_operation_log_target` (`target_type`,`target_public_id`),
  KEY `idx_admin_operation_log_admin` (`admin_user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志';

CREATE TABLE `file_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_type` varchar(64) NOT NULL,
  `storage_provider` varchar(32) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `storage_path` varchar(255) NOT NULL,
  `public_url` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件资产';

INSERT INTO `admin_user` (`id`, `username`, `password`, `name`, `status`)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 1);

INSERT INTO `brewery` (`id`, `company_name`, `contact_name`, `phone`, `wechat`)
VALUES (1, '山海精酿', '张三', '13800000001', 'brewery-zhangsan');

INSERT INTO `portal_account` (`id`, `phone`, `wechat`, `display_name`, `brewery_id`, `status`)
VALUES (1, '13800000001', 'brewery-zhangsan', '山海精酿联系人', 1, 1);

INSERT INTO `competition` (`id`, `code`, `name`, `edition`, `competition_date`, `registration_start`, `registration_deadline`, `status`, `entry_fee`)
VALUES (1, 'BC-2026', '2026中国精酿啤酒大赛', '第三批次', '2026-08-18', '2026-06-01 10:00:00', '2026-07-31 23:59:59', 'REGISTRATION_OPEN', 299.00);

INSERT INTO `competition_category` (`id`, `competition_id`, `name`, `sort_order`)
VALUES
  (1, 1, 'IPA', 1),
  (2, 1, '拉格', 2);

INSERT INTO `competition_style_config` (`id`, `competition_id`, `name`, `sort_order`)
VALUES
  (1, 1, 'American IPA', 1),
  (2, 1, 'Double IPA', 2),
  (3, 1, 'Pilsner', 3);

INSERT INTO `entry_field_config` (`id`, `competition_id`, `field_key`, `field_label`, `field_type`, `required_flag`, `visible_to_judges`, `sort_order`)
VALUES
  (1, 1, 'specialIngredients', '增味原料或特殊工艺', 'textarea', 0, 1, 1);

INSERT INTO `competition_score_config` (`id`, `competition_id`, `judge_role_type`, `dimensions_json`)
VALUES
  (1, 1, 'CROSS', JSON_ARRAY(
    JSON_OBJECT('key', 'impression', 'label', '整体感受', 'maxScore', 20),
    JSON_OBJECT('key', 'drinkability', 'label', '适饮性', 'maxScore', 20),
    JSON_OBJECT('key', 'story', 'label', '记忆点', 'maxScore', 10)
  )),
  (2, 1, 'PROFESSIONAL', JSON_ARRAY(
    JSON_OBJECT('key', 'aroma', 'label', '香气', 'maxScore', 12),
    JSON_OBJECT('key', 'appearance', 'label', '外观', 'maxScore', 3),
    JSON_OBJECT('key', 'flavor', 'label', '味道', 'maxScore', 20),
    JSON_OBJECT('key', 'mouthfeel', 'label', '口感', 'maxScore', 5),
    JSON_OBJECT('key', 'overall', 'label', '整体印象', 'maxScore', 10)
  )),
  (3, 1, 'CAPTAIN', JSON_ARRAY(
    JSON_OBJECT('key', 'consensus', 'label', '共识评分', 'maxScore', 50)
  ));

INSERT INTO `judge_table` (`id`, `competition_id`, `table_name`)
VALUES (1, 1, 'A桌');

INSERT INTO `beer_entry` (`id`, `uuid`, `competition_id`, `brewery_id`, `category_id`, `name`, `style`, `abv`, `description`, `extra_fields_json`, `status`)
VALUES
  (1, 'BC-2026-IPA-0001', 1, 1, 1, '海风双倍IPA', 'Double IPA', 7.5, '柑橘、松针与热带水果香气明显，苦度干净。', JSON_OBJECT('specialIngredients', '西楚酒花'), 'REGISTERED');

INSERT INTO `beer_entry_extra_field` (`beer_entry_id`, `field_key`, `field_label`, `field_value`)
VALUES (1, 'specialIngredients', '增味原料或特殊工艺', '西楚酒花');
