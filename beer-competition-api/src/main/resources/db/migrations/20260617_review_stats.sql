CREATE TABLE IF NOT EXISTS `judge_score_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `round_id` bigint NOT NULL,
  `round_table_id` bigint NOT NULL,
  `beer_entry_id` bigint NOT NULL,
  `judge_account_id` bigint NOT NULL,
  `judge_role_type` varchar(32) NOT NULL,
  `started_at` datetime DEFAULT NULL,
  `first_submitted_at` datetime DEFAULT NULL,
  `last_submitted_at` datetime DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `comment_char_count` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_judge_score_session_round_entry_judge_role` (`round_table_id`,`beer_entry_id`,`judge_account_id`,`judge_role_type`),
  KEY `idx_judge_score_session_round` (`round_id`),
  KEY `idx_judge_score_session_judge` (`judge_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评审评分过程会话';

ALTER TABLE `score_record`
  ADD COLUMN `round_id` bigint DEFAULT NULL AFTER `competition_id`,
  ADD COLUMN `round_table_id` bigint DEFAULT NULL AFTER `round_id`,
  ADD COLUMN `duration_seconds` int DEFAULT NULL AFTER `consensus_score`,
  ADD COLUMN `comment_char_count` int NOT NULL DEFAULT '0' AFTER `duration_seconds`;
