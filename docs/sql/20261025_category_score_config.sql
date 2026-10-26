-- 组别专属评分表：category_id = 0 表示通用评分表，> 0 表示指定投递组别。
SET @has_category_id := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_score_config' AND COLUMN_NAME = 'category_id'
);
SET @sql := IF(@has_category_id = 0,
  'ALTER TABLE competition_score_config ADD COLUMN category_id BIGINT NOT NULL DEFAULT 0 COMMENT ''0=通用评分表；>0=组别ID'' AFTER competition_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @score_index_columns := (
  SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_score_config' AND INDEX_NAME = 'uk_competition_score_config'
);
SET @sql := IF(COALESCE(@score_index_columns, '') <> 'competition_id,category_id,judge_role_type',
  'ALTER TABLE competition_score_config DROP INDEX uk_competition_score_config, ADD UNIQUE KEY uk_competition_score_config (competition_id, category_id, judge_role_type)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'category score config migration completed' AS migration_status;
