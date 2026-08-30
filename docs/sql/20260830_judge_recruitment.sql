CREATE TABLE IF NOT EXISTS judge_recruitment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id VARCHAR(64) NOT NULL,
  competition_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  recruitment_start DATETIME NOT NULL,
  recruitment_deadline DATETIME NOT NULL,
  venue VARCHAR(255) NOT NULL,
  address VARCHAR(500) DEFAULT NULL,
  description TEXT,
  requirements TEXT,
  expected_count INT DEFAULT NULL,
  created_by BIGINT DEFAULT NULL,
  closed_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_judge_recruitment_public_id (public_id),
  UNIQUE KEY uk_judge_recruitment_competition (competition_id),
  KEY idx_judge_recruitment_status_window (status, recruitment_start, recruitment_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS judge_recruitment_application (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id VARCHAR(64) NOT NULL,
  recruitment_id BIGINT NOT NULL,
  judge_account_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'APPLIED',
  availability_confirmed TINYINT NOT NULL DEFAULT 0,
  note VARCHAR(1000) DEFAULT NULL,
  review_remark VARCHAR(1000) DEFAULT NULL,
  processed_by BIGINT DEFAULT NULL,
  processed_time DATETIME DEFAULT NULL,
  withdrawn_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_judge_recruitment_application_public_id (public_id),
  UNIQUE KEY uk_judge_recruitment_application_judge (recruitment_id, judge_account_id),
  KEY idx_judge_recruitment_application_status (recruitment_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @add_reopened_time = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE judge_recruitment ADD COLUMN reopened_time DATETIME NULL AFTER closed_time',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'judge_recruitment'
    AND column_name = 'reopened_time'
);
PREPARE stmt_add_reopened_time FROM @add_reopened_time;
EXECUTE stmt_add_reopened_time;
DEALLOCATE PREPARE stmt_add_reopened_time;

SET @add_recruitment_column = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_judge_assignment ADD COLUMN recruitment_application_id BIGINT NULL',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_judge_assignment'
    AND column_name = 'recruitment_application_id'
);
PREPARE stmt_add_recruitment_column FROM @add_recruitment_column;
EXECUTE stmt_add_recruitment_column;
DEALLOCATE PREPARE stmt_add_recruitment_column;

SET @add_recruitment_index = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE competition_judge_assignment ADD KEY idx_judge_assignment_recruitment_application (recruitment_application_id)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_judge_assignment'
    AND index_name = 'idx_judge_assignment_recruitment_application'
);
PREPARE stmt_add_recruitment_index FROM @add_recruitment_index;
EXECUTE stmt_add_recruitment_index;
DEALLOCATE PREPARE stmt_add_recruitment_index;
