USE `beer_competition`;

DROP TABLE IF EXISTS `round_result`;
DROP TABLE IF EXISTS `round_table_entry`;
DROP TABLE IF EXISTS `round_table_member`;
DROP TABLE IF EXISTS `round_table`;
DROP TABLE IF EXISTS `competition_round`;
DROP TABLE IF EXISTS `competition_judge_assignment`;
DROP TABLE IF EXISTS `competition_judge_table`;
DROP TABLE IF EXISTS `judge_assignment`;
DROP TABLE IF EXISTS `judge_table`;

ALTER TABLE `beer_entry`
  ADD COLUMN IF NOT EXISTS `short_code` varchar(16) DEFAULT NULL AFTER `uuid`,
  ADD COLUMN IF NOT EXISTS `stored_flag` tinyint NOT NULL DEFAULT 0 AFTER `status`;

CREATE TABLE `competition_judge_table` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `table_name` varchar(64) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_judge_table_name` (`competition_id`,`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛基础评审桌';

CREATE TABLE `competition_judge_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `base_table_id` bigint NOT NULL,
  `judge_account_id` bigint NOT NULL,
  `role` varchar(32) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_judge_assignment` (`competition_id`,`judge_account_id`),
  KEY `idx_competition_judge_assignment_table` (`base_table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛基础评审分配';

CREATE TABLE `competition_round` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `round_no` int NOT NULL,
  `round_name` varchar(64) NOT NULL,
  `round_type` varchar(32) NOT NULL,
  `source_round_id` bigint DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `published_time` datetime DEFAULT NULL,
  `submitted_time` datetime DEFAULT NULL,
  `locked_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_round_no` (`competition_id`,`round_no`),
  KEY `idx_competition_round_source` (`source_round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛轮次';

CREATE TABLE `round_table` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `round_id` bigint NOT NULL,
  `table_name` varchar(64) NOT NULL,
  `captain_judge_id` bigint DEFAULT NULL,
  `target_count` int NOT NULL DEFAULT 1,
  `target_mode` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_table_name` (`round_id`,`table_name`),
  KEY `idx_round_table_competition` (`competition_id`),
  KEY `idx_round_table_captain` (`captain_judge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮次桌任务';

CREATE TABLE `round_table_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `round_table_id` bigint NOT NULL,
  `judge_account_id` bigint NOT NULL,
  `role` varchar(32) NOT NULL,
  `system_task_required` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_table_member` (`round_table_id`,`judge_account_id`),
  KEY `idx_round_table_member_judge` (`judge_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮次桌参与人';

CREATE TABLE `round_table_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `round_id` bigint NOT NULL,
  `round_table_id` bigint NOT NULL,
  `beer_entry_id` bigint NOT NULL,
  `source_round_table_id` bigint DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_entry_once` (`round_id`,`beer_entry_id`),
  KEY `idx_round_table_entry_table` (`round_table_id`),
  KEY `idx_round_table_entry_competition` (`competition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮次桌酒款';

CREATE TABLE `round_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `round_id` bigint NOT NULL,
  `round_table_id` bigint NOT NULL,
  `beer_entry_id` bigint NOT NULL,
  `result_type` varchar(32) NOT NULL,
  `rank_no` int DEFAULT NULL,
  `slot_label` varchar(64) DEFAULT NULL,
  `submitted_by` bigint DEFAULT NULL,
  `submitted_time` datetime DEFAULT NULL,
  `locked_flag` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_result_slot` (`round_table_id`,`result_type`,`rank_no`),
  KEY `idx_round_result_entry` (`beer_entry_id`),
  KEY `idx_round_result_competition` (`competition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮次结果';
