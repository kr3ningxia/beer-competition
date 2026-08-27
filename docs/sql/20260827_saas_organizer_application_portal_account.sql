-- 为登录厂商账号关联主办方入驻申请。
-- 历史公开申请保持 NULL，继续通过申请编号和手机号查询。
-- 执行方式：mysql --default-character-set=utf8mb4 ... < 20260827_saas_organizer_application_portal_account.sql

SET NAMES utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS `saas_add_organizer_application_portal_account`$$
CREATE PROCEDURE `saas_add_organizer_application_portal_account`()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'organizer_application'
       AND column_name = 'portal_account_id'
  ) THEN
    ALTER TABLE `organizer_application`
      ADD COLUMN `portal_account_id` bigint DEFAULT NULL COMMENT '提交申请的厂商账号' AFTER `id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'organizer_application'
       AND index_name = 'idx_organizer_application_portal_account'
  ) THEN
    ALTER TABLE `organizer_application`
      ADD KEY `idx_organizer_application_portal_account` (`portal_account_id`);
  END IF;
END$$
DELIMITER ;

CALL `saas_add_organizer_application_portal_account`();
DROP PROCEDURE `saas_add_organizer_application_portal_account`;

SELECT 'organizer_application.portal_account_id migration completed' AS migration_status;
SHOW COLUMNS FROM `organizer_application` LIKE 'portal_account_id';
SHOW INDEX FROM `organizer_application` WHERE Key_name = 'idx_organizer_application_portal_account';
