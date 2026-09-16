-- 厂商通知邮箱、赛事邮件通知规则、模板和发送队列
SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE portal_account ADD COLUMN email_enc VARCHAR(512) NULL COMMENT ''邮箱加密值'', ADD COLUMN email_hash VARCHAR(64) NULL COMMENT ''邮箱摘要'', ADD COLUMN email_last4 VARCHAR(8) NULL COMMENT ''邮箱本地部分末4位'', ADD COLUMN email_bounce_status VARCHAR(32) NULL COMMENT ''邮箱退信状态''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'portal_account' AND column_name = 'email_enc'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE portal_account ADD UNIQUE KEY uk_portal_account_email_hash (email_hash)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'portal_account' AND index_name = 'uk_portal_account_email_hash'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 早期版本曾用邮箱验证码校验邮箱，现改为填写即视为可投递，清理旧结构
DROP TABLE IF EXISTS portal_email_verification;

SET @ddl = (
  SELECT IF(COUNT(*) = 1,
    'ALTER TABLE portal_account DROP COLUMN email_verified',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'portal_account' AND column_name = 'email_verified'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 1,
    'ALTER TABLE portal_account DROP COLUMN email_verified_time',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'portal_account' AND column_name = 'email_verified_time'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS notification_template (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  event_code VARCHAR(64) NOT NULL COMMENT '通知事件',
  name VARCHAR(128) NOT NULL COMMENT '模板名称',
  version INT NOT NULL COMMENT '模板版本',
  subject VARCHAR(200) NOT NULL COMMENT '邮件主题',
  html_body MEDIUMTEXT NOT NULL COMMENT 'HTML正文',
  status VARCHAR(32) NOT NULL COMMENT '模板状态',
  created_by BIGINT NULL COMMENT '创建管理员ID',
  published_time DATETIME NULL COMMENT '启用时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_template_event_version (event_code, version),
  KEY idx_notification_template_event_status (event_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件通知模板';

CREATE TABLE IF NOT EXISTS competition_notification_rule (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  competition_id BIGINT NOT NULL COMMENT '比赛ID',
  event_code VARCHAR(64) NOT NULL COMMENT '通知事件',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  schedule_mode VARCHAR(32) NOT NULL COMMENT '发送方式',
  scheduled_at DATETIME NULL COMMENT '固定发送时间',
  offset_minutes INT NULL COMMENT '相对节点分钟数',
  send_time VARCHAR(5) NOT NULL DEFAULT '10:00' COMMENT '相对节点发送时刻',
  timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '时区',
  template_id BIGINT NULL COMMENT '模板版本ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_competition_notification_rule (competition_id, event_code),
  KEY idx_competition_notification_rule_due (enabled, schedule_mode, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛邮件通知规则';

CREATE TABLE IF NOT EXISTS email_delivery (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  competition_id BIGINT NOT NULL COMMENT '比赛ID',
  event_code VARCHAR(64) NOT NULL COMMENT '通知事件',
  recipient_account_id BIGINT NOT NULL COMMENT '厂商账号ID',
  recipient_email_enc VARCHAR(512) NOT NULL COMMENT '收件邮箱加密值',
  recipient_email_hash VARCHAR(64) NOT NULL COMMENT '收件邮箱摘要',
  result_version INT NOT NULL DEFAULT 1 COMMENT '结果版本',
  subject_snapshot VARCHAR(200) NOT NULL COMMENT '主题快照',
  html_body_snapshot MEDIUMTEXT NOT NULL COMMENT '正文快照',
  scheduled_time DATETIME NOT NULL COMMENT '计划发送时间',
  status VARCHAR(32) NOT NULL COMMENT '发送状态',
  attempt_count INT NOT NULL DEFAULT 0 COMMENT '尝试次数',
  next_retry_time DATETIME NULL COMMENT '下次重试时间',
  provider_request_id VARCHAR(128) NULL COMMENT '供应商请求编号',
  last_error VARCHAR(1000) NULL COMMENT '最近错误',
  sent_time DATETIME NULL COMMENT '发送时间',
  delivered_time DATETIME NULL COMMENT '投递时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_email_delivery_dedup (competition_id, event_code, recipient_account_id, result_version),
  KEY idx_email_delivery_pending (status, scheduled_time, next_retry_time),
  KEY idx_email_delivery_competition (competition_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件发送队列与日志';

SELECT 'email notification migration completed' AS migration_status;
