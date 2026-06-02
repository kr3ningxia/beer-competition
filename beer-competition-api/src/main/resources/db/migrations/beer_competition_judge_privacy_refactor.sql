USE `beer_competition`;

DELETE FROM `score_record`;
DELETE FROM `judge_assignment`;
DELETE FROM `judge_account`;

ALTER TABLE `judge_account`
  DROP INDEX `uk_judge_account_phone`;

ALTER TABLE `judge_account`
  DROP COLUMN `phone`,
  DROP COLUMN `wechat`;

ALTER TABLE `judge_account`
  ADD COLUMN `public_id` varchar(32) NOT NULL AFTER `id`,
  ADD COLUMN `phone_enc` text NOT NULL AFTER `public_id`,
  ADD COLUMN `phone_hash` char(64) NOT NULL AFTER `phone_enc`,
  ADD COLUMN `phone_last4` varchar(4) DEFAULT NULL AFTER `phone_hash`,
  ADD COLUMN `wechat_enc` text DEFAULT NULL AFTER `phone_last4`,
  ADD UNIQUE KEY `uk_judge_account_public_id` (`public_id`),
  ADD UNIQUE KEY `uk_judge_account_phone_hash` (`phone_hash`),
  ADD KEY `idx_judge_account_phone_last4` (`phone_last4`);

CREATE TABLE IF NOT EXISTS `admin_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint NOT NULL,
  `action` varchar(64) NOT NULL,
  `target_type` varchar(32) NOT NULL,
  `target_public_id` varchar(64) DEFAULT NULL,
  `summary` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_operation_log_target` (`target_type`,`target_public_id`),
  KEY `idx_admin_operation_log_admin` (`admin_user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志';

ALTER TABLE `sms_code_log`
  DROP COLUMN `phone`,
  DROP COLUMN `code`,
  ADD COLUMN `phone_hash` char(64) NOT NULL AFTER `id`,
  ADD COLUMN `masked_phone` varchar(20) NOT NULL AFTER `phone_hash`,
  ADD KEY `idx_sms_code_log_phone_hash` (`phone_hash`);
