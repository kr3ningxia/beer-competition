-- 排序草稿和确认记录幂等修复。先执行 20261011_multi_category_medal_round.sql。
-- 执行期间暂停评审写入，完成后部署新版后端。脚本可重复执行。
-- 历史重复草稿保留最新 id；重复确认按同一桌、组别、评委、版本保留最新 id。

DELETE older FROM round_judge_ranking_draft older
JOIN round_judge_ranking_draft newer
  ON newer.round_table_id = older.round_table_id
 AND newer.category_id <=> older.category_id
 AND newer.judge_account_id = older.judge_account_id
 AND newer.id > older.id;

DELETE older FROM round_table_confirmation older
JOIN round_table_confirmation newer
  ON newer.round_table_id = older.round_table_id
 AND newer.category_id <=> older.category_id
 AND newer.judge_account_id = older.judge_account_id
 AND newer.result_version = older.result_version
 AND newer.id > older.id;

SET @sql := IF(EXISTS (
  SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'round_judge_ranking_draft' AND COLUMN_NAME = 'category_key'
), 'SELECT 1',
  'ALTER TABLE round_judge_ranking_draft ADD COLUMN category_key BIGINT GENERATED ALWAYS AS (COALESCE(category_id, 0)) STORED COMMENT ''用于草稿唯一约束的组别键'' AFTER category_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS (
  SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'round_table_confirmation' AND COLUMN_NAME = 'category_key'
), 'SELECT 1',
  'ALTER TABLE round_table_confirmation ADD COLUMN category_key BIGINT GENERATED ALWAYS AS (COALESCE(category_id, 0)) STORED COMMENT ''用于确认唯一约束的组别键'' AFTER category_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 将可空 category_id 的旧唯一键替换为归一后的 category_key；一次 ALTER 完成替换。
SET @sql := IF(EXISTS (
  SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'round_judge_ranking_draft'
    AND INDEX_NAME = 'uk_round_judge_ranking_draft' AND COLUMN_NAME = 'category_key'
), 'SELECT 1',
  'ALTER TABLE round_judge_ranking_draft DROP INDEX uk_round_judge_ranking_draft, ADD UNIQUE KEY uk_round_judge_ranking_draft (round_table_id, category_key, judge_account_id)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS (
  SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'round_table_confirmation'
    AND INDEX_NAME = 'uk_round_table_confirmation_version' AND COLUMN_NAME = 'category_key'
), 'SELECT 1',
  'ALTER TABLE round_table_confirmation DROP INDEX uk_round_table_confirmation_version, ADD UNIQUE KEY uk_round_table_confirmation_version (round_table_id, category_key, judge_account_id, result_version)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'ranking draft and confirmation idempotency migration completed' AS migration_status;
