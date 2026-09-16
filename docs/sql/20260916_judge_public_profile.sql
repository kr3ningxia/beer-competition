-- 评委公开资料与赛事发布时的评委快照
SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE judge_account ADD COLUMN avatar_asset_id BIGINT NULL COMMENT ''评委头像文件资产ID''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'judge_account'
    AND column_name = 'avatar_asset_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE judge_account ADD COLUMN public_profile_consent TINYINT NOT NULL DEFAULT 0 COMMENT ''是否同意赛事结果公开评委资料''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'judge_account'
    AND column_name = 'public_profile_consent'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE judge_account ADD COLUMN public_profile_consent_time DATETIME NULL COMMENT ''公开评委资料授权时间''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'judge_account'
    AND column_name = 'public_profile_consent_time'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS competition_judge_public_profile (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  competition_id BIGINT NOT NULL COMMENT '比赛ID',
  judge_account_id BIGINT NOT NULL COMMENT '评委账号ID',
  name VARCHAR(64) NOT NULL COMMENT '发布时公开姓名',
  qualification VARCHAR(255) NULL COMMENT '发布时公开资质',
  avatar_asset_id BIGINT NULL COMMENT '发布时头像文件资产ID',
  roles_json VARCHAR(1024) NOT NULL COMMENT '发布时公开角色JSON',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '公开展示顺序',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_competition_judge_public_profile (competition_id, judge_account_id),
  KEY idx_competition_judge_public_profile_avatar (avatar_asset_id),
  KEY idx_competition_judge_public_profile_competition (competition_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='赛事公开评委资料快照';
