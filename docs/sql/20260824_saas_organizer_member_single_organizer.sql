-- 主办方成员关系增量约束。
-- 执行前检查重复账号；本脚本只收紧数据模型，不删除业务数据。
-- 执行方式：mysql --default-character-set=utf8mb4 ... < 20260824_saas_organizer_member_single_organizer.sql

SET NAMES utf8mb4;

SELECT `admin_user_id`, COUNT(*) AS `member_count`
  FROM `organizer_member`
 GROUP BY `admin_user_id`
HAVING COUNT(*) > 1;

SET @has_duplicate_member_admin = (
  SELECT EXISTS(
    SELECT 1
      FROM `organizer_member`
     GROUP BY `admin_user_id`
    HAVING COUNT(*) > 1
  )
);

-- 重复关系必须先人工处理，避免错误约束改变现有账号归属。
SET @constraint_sql = IF(
  @has_duplicate_member_admin = 0,
  'ALTER TABLE `organizer_member` ADD UNIQUE KEY `uk_organizer_member_admin_user` (`admin_user_id`)',
  'SELECT ''organizer_member contains duplicate admin_user_id; constraint not applied'' AS migration_status'
);
PREPARE constraint_statement FROM @constraint_sql;
EXECUTE constraint_statement;
DEALLOCATE PREPARE constraint_statement;

SELECT 'saas organizer member single-organizer constraint checked' AS migration_status;
