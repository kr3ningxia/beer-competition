-- 评审日期沿用 competition.competition_date，此字段只保存当天的开始时刻。
-- 旧招募保留 NULL，避免为未设置时刻的比赛自动补入 08:00。
SET @add_judging_start_time = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE judge_recruitment ADD COLUMN judging_start_time TIME NULL AFTER recruitment_deadline',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'judge_recruitment'
    AND column_name = 'judging_start_time'
);
PREPARE stmt_add_judging_start_time FROM @add_judging_start_time;
EXECUTE stmt_add_judging_start_time;
DEALLOCATE PREPARE stmt_add_judging_start_time;
