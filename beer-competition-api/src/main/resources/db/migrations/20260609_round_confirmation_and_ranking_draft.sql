ALTER TABLE `round_table`
  ADD COLUMN `result_version` int NOT NULL DEFAULT 0 AFTER `status`,
  ADD COLUMN `confirmation_override_flag` tinyint NOT NULL DEFAULT 0 AFTER `result_version`,
  ADD COLUMN `confirmation_override_reason` varchar(255) NULL DEFAULT NULL AFTER `confirmation_override_flag`,
  ADD COLUMN `confirmation_override_by` bigint NULL DEFAULT NULL AFTER `confirmation_override_reason`,
  ADD COLUMN `confirmation_override_time` datetime NULL DEFAULT NULL AFTER `confirmation_override_by`;

CREATE TABLE IF NOT EXISTS `round_table_confirmation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `round_table_id` bigint NOT NULL,
  `judge_account_id` bigint NOT NULL,
  `result_version` int NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'AGREED',
  `confirmed_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_table_confirmation_version` (`round_table_id`, `judge_account_id`, `result_version`),
  KEY `idx_round_table_confirmation_judge` (`judge_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='本桌结果评审确认';

CREATE TABLE IF NOT EXISTS `round_judge_ranking_draft` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `round_id` bigint NOT NULL,
  `round_table_id` bigint NOT NULL,
  `judge_account_id` bigint NOT NULL,
  `rankings_json` json NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_judge_ranking_draft` (`round_table_id`, `judge_account_id`),
  KEY `idx_round_judge_ranking_draft_round` (`round_id`),
  KEY `idx_round_judge_ranking_draft_judge` (`judge_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='排序轮个人参考排序';
