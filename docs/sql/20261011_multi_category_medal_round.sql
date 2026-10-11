-- 多组别金银铜轮：结果、确认按桌内组别隔离。
-- 先在低流量窗口执行；历史单组别记录的 category_id 保持 NULL 以兼容旧赛事。
SET @has_round_result_category := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'round_result' AND COLUMN_NAME = 'category_id'
);
SET @sql := IF(@has_round_result_category = 0,
  'ALTER TABLE round_result ADD COLUMN category_id BIGINT NULL COMMENT ''桌内组别ID，旧单组别结果可为空'' AFTER round_table_id',
  'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE round_result DROP INDEX uk_round_result_slot;
ALTER TABLE round_result ADD UNIQUE KEY uk_round_result_slot (round_table_id, category_id, result_type, rank_no);

SET @has_confirmation_category := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'round_table_confirmation' AND COLUMN_NAME = 'category_id'
);
SET @sql := IF(@has_confirmation_category = 0,
  'ALTER TABLE round_table_confirmation ADD COLUMN category_id BIGINT NULL COMMENT ''桌内组别ID，旧单组别确认可为空'' AFTER round_table_id',
  'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE round_table_confirmation DROP INDEX uk_round_table_confirmation_version;
ALTER TABLE round_table_confirmation ADD UNIQUE KEY uk_round_table_confirmation_version (round_table_id, category_id, judge_account_id, result_version);

SET @has_draft_category := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'round_judge_ranking_draft' AND COLUMN_NAME = 'category_id'
);
SET @sql := IF(@has_draft_category = 0,
  'ALTER TABLE round_judge_ranking_draft ADD COLUMN category_id BIGINT NULL COMMENT ''桌内组别ID，旧单组别草稿可为空'' AFTER round_table_id',
  'SELECT 1'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE round_judge_ranking_draft DROP INDEX uk_round_judge_ranking_draft;
ALTER TABLE round_judge_ranking_draft ADD UNIQUE KEY uk_round_judge_ranking_draft (round_table_id, category_id, judge_account_id);

CREATE TABLE IF NOT EXISTS round_table_category_state (
  id BIGINT NOT NULL AUTO_INCREMENT,
  round_table_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  result_version INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_round_table_category_state (round_table_id, category_id),
  KEY idx_round_table_category_state_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SELECT 'multi-category medal round migration completed' AS migration_status;
