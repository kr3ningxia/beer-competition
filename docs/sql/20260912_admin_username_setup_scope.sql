-- 收窄"首次登录必须修改登录账号"的适用范围：仅平台发放的初始账号（initial_credential_issued_time 非空）。
-- 内部由管理员新建的账号，登录名由创建者设定，只保留首次改密要求。
-- 执行方式：mysql --default-character-set=utf8mb4 ... < 20260912_admin_username_setup_scope.sql

SET NAMES utf8mb4;

UPDATE `admin_user`
   SET `must_change_username` = 0
 WHERE `must_change_username` = 1
   AND `initial_credential_issued_time` IS NULL;

SELECT 'admin username setup scope migration completed' AS migration_status;
SELECT COUNT(*) AS pending_username_setup
  FROM `admin_user`
 WHERE `must_change_username` = 1;
