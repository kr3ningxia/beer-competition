-- 管理员首次账号初始化：首次发放账号必须设置新的登录账号和密码。
-- 执行方式：mysql --default-character-set=utf8mb4 ... < 20260827_admin_credentials_setup.sql

SET NAMES utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS `saas_add_admin_username_setup_flag`$$
CREATE PROCEDURE `saas_add_admin_username_setup_flag`()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'admin_user'
       AND column_name = 'must_change_username'
  ) THEN
    ALTER TABLE `admin_user`
      ADD COLUMN `must_change_username` tinyint NOT NULL DEFAULT 0
      COMMENT '是否必须首次修改登录账号'
      AFTER `must_change_password`;
  END IF;
END$$
DELIMITER ;

CALL `saas_add_admin_username_setup_flag`();
DROP PROCEDURE `saas_add_admin_username_setup_flag`;

-- 兼容已经发放但尚未完成首次改密的历史账号；已完成改密的账号不重新触发用户名设置。
UPDATE `admin_user`
   SET `must_change_username` = 1
 WHERE `must_change_password` = 1
   AND `initial_credential_issued_time` IS NOT NULL
   AND `must_change_username` = 0;

SELECT 'admin credential setup migration completed' AS migration_status;
SHOW COLUMNS FROM `admin_user` LIKE 'must_change_username';
SELECT COUNT(*) AS pending_username_setup
  FROM `admin_user`
 WHERE `must_change_username` = 1;
