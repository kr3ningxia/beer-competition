-- 评委本场在场状态：保留历史分配与评分，支持离场后重新加入。
SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_judge_assignment ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT ''ACTIVE'' COMMENT ''本场分配状态 ACTIVE/WITHDRAWN''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_judge_assignment'
    AND column_name = 'status'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_judge_assignment ADD COLUMN withdrawn_time DATETIME NULL',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_judge_assignment'
    AND column_name = 'withdrawn_time'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_judge_assignment ADD COLUMN withdrawn_by BIGINT NULL',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_judge_assignment'
    AND column_name = 'withdrawn_by'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_judge_assignment ADD COLUMN withdraw_reason VARCHAR(255) NULL',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_judge_assignment'
    AND column_name = 'withdraw_reason'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE round_table_member ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT ''ACTIVE'' COMMENT ''轮次成员状态 ACTIVE/REMOVED''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'round_table_member'
    AND column_name = 'status'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE round_table_member ADD COLUMN removed_time DATETIME NULL',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'round_table_member'
    AND column_name = 'removed_time'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE round_table_member ADD COLUMN removed_by BIGINT NULL',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'round_table_member'
    AND column_name = 'removed_by'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE round_table_member ADD COLUMN remove_reason VARCHAR(255) NULL',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'round_table_member'
    AND column_name = 'remove_reason'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'CREATE INDEX idx_judge_assignment_competition_status ON competition_judge_assignment (competition_id, status)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_judge_assignment'
    AND index_name = 'idx_judge_assignment_competition_status'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'CREATE INDEX idx_round_table_member_status ON round_table_member (round_table_id, status)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'round_table_member'
    AND index_name = 'idx_round_table_member_status'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
