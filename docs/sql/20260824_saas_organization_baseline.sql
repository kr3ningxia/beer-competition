-- SaaS 组织与比赛归属基线迁移。
-- 执行前必须完成数据库备份；本脚本只新增结构、索引和回填数据，不删除一期数据。
-- 执行方式：mysql --default-character-set=utf8mb4 ... < 20260824_saas_organization_baseline.sql

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS `enterprise_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '企业账户主键',
  `account_code` varchar(64) NOT NULL COMMENT '企业账户编号',
  `name` varchar(128) NOT NULL COMMENT '企业账户名称',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enterprise_account_code` (`account_code`),
  KEY `idx_enterprise_account_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用企业账户';

CREATE TABLE IF NOT EXISTS `organizer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主办方主键',
  `enterprise_account_id` bigint NOT NULL COMMENT '关联企业账户',
  `name` varchar(128) NOT NULL COMMENT '主办方名称',
  `organizer_type` varchar(32) NOT NULL COMMENT 'PLATFORM 或 TENANT',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '组织状态',
  `logo_asset_id` bigint DEFAULT NULL COMMENT '组织 Logo 文件',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(64) DEFAULT NULL COMMENT '联系人电话（按业务需要加密）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_organizer_enterprise_type` (`enterprise_account_id`,`organizer_type`),
  KEY `idx_organizer_type_status` (`organizer_type`,`status`),
  KEY `idx_organizer_logo_asset` (`logo_asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='赛事主办方组织';

CREATE TABLE IF NOT EXISTS `organizer_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员关系主键',
  `organizer_id` bigint NOT NULL COMMENT '主办方组织',
  `admin_user_id` bigint NOT NULL COMMENT '后台账号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_organizer_member` (`organizer_id`,`admin_user_id`),
  KEY `idx_organizer_member_admin` (`admin_user_id`,`status`),
  KEY `idx_organizer_member_organizer` (`organizer_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='主办方管理员成员关系';

CREATE TABLE IF NOT EXISTS `organizer_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '入驻申请主键',
  `portal_account_id` bigint DEFAULT NULL COMMENT '提交申请的厂商账号',
  `application_no` varchar(64) NOT NULL COMMENT '申请编号',
  `organization_name` varchar(128) NOT NULL COMMENT '申请主体名称',
  `contact_name` varchar(64) NOT NULL COMMENT '联系人姓名',
  `contact_phone_enc` varchar(255) NOT NULL COMMENT '联系人手机号密文',
  `contact_phone_hash` varchar(128) NOT NULL COMMENT '联系人手机号摘要',
  `contact_phone_last4` varchar(8) DEFAULT NULL COMMENT '联系人手机号后四位',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '联系人邮箱',
  `wechat_enc` varchar(255) DEFAULT NULL COMMENT '微信号密文',
  `business_description` varchar(1000) DEFAULT NULL COMMENT '办赛简介',
  `expected_scale` varchar(255) DEFAULT NULL COMMENT '预计赛事规模',
  `supplemental_note` varchar(1000) DEFAULT NULL COMMENT '补充说明',
  `material_asset_id` bigint DEFAULT NULL COMMENT '主体证明材料文件',
  `status` varchar(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '申请状态',
  `reviewed_by_admin_id` bigint DEFAULT NULL COMMENT '审核管理员',
  `reviewed_time` datetime DEFAULT NULL COMMENT '审核时间',
  `review_remark` varchar(1000) DEFAULT NULL COMMENT '审核备注',
  `organizer_id` bigint DEFAULT NULL COMMENT '开通后的主办方',
  `initial_admin_user_id` bigint DEFAULT NULL COMMENT '开通后的初始管理员',
  `account_issued_time` datetime DEFAULT NULL COMMENT '账号发放时间',
  `submitted_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_organizer_application_no` (`application_no`),
  KEY `idx_organizer_application_status` (`status`,`submitted_time`,`id`),
  KEY `idx_organizer_application_organizer` (`organizer_id`),
  KEY `idx_organizer_application_phone` (`contact_phone_hash`),
  KEY `idx_organizer_application_portal_account` (`portal_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='主办方入驻申请';

-- MySQL 8.1 不支持 ALTER TABLE 的 ADD COLUMN IF NOT EXISTS，使用元数据检查保证重复执行安全。
DELIMITER $$
DROP PROCEDURE IF EXISTS `saas_add_column_if_missing`$$
CREATE PROCEDURE `saas_add_column_if_missing`(
  IN p_table varchar(64),
  IN p_column varchar(64),
  IN p_definition text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND column_name = p_column
  ) THEN
    SET @saas_sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_definition);
    PREPARE saas_stmt FROM @saas_sql;
    EXECUTE saas_stmt;
    DEALLOCATE PREPARE saas_stmt;
  END IF;
END$$
DELIMITER ;

CALL `saas_add_column_if_missing`('admin_user', 'admin_type', '`admin_type` varchar(32) NOT NULL DEFAULT ''PLATFORM_SUPER_ADMIN'' COMMENT ''后台操作身份'' AFTER `status`');
CALL `saas_add_column_if_missing`('admin_user', 'must_change_password', '`must_change_password` tinyint NOT NULL DEFAULT 0 COMMENT ''是否必须首次改密'' AFTER `admin_type`');
CALL `saas_add_column_if_missing`('admin_user', 'must_change_username', '`must_change_username` tinyint NOT NULL DEFAULT 0 COMMENT ''是否必须首次修改登录账号'' AFTER `must_change_password`');
CALL `saas_add_column_if_missing`('admin_user', 'initial_credential_issued_time', '`initial_credential_issued_time` datetime DEFAULT NULL COMMENT ''初始凭据发放时间'' AFTER `must_change_password`');
CALL `saas_add_column_if_missing`('competition', 'organizer_id', '`organizer_id` bigint DEFAULT NULL COMMENT ''比赛所属主办方'' AFTER `id`');
CALL `saas_add_column_if_missing`('file_asset', 'organizer_id', '`organizer_id` bigint DEFAULT NULL COMMENT ''文件所属主办方'' AFTER `id`');
CALL `saas_add_column_if_missing`('admin_operation_log', 'organizer_id', '`organizer_id` bigint DEFAULT NULL COMMENT ''操作所属主办方'' AFTER `admin_user_id`');
CALL `saas_add_column_if_missing`('admin_operation_log', 'competition_id', '`competition_id` bigint DEFAULT NULL COMMENT ''相关比赛'' AFTER `organizer_id`');
CALL `saas_add_column_if_missing`('organizer_application', 'portal_account_id', '`portal_account_id` bigint DEFAULT NULL COMMENT ''提交申请的厂商账号'' AFTER `id`');
DROP PROCEDURE `saas_add_column_if_missing`;

DELIMITER $$
DROP PROCEDURE IF EXISTS `saas_add_index_if_missing`$$
CREATE PROCEDURE `saas_add_index_if_missing`(
  IN p_table varchar(64),
  IN p_index varchar(64),
  IN p_definition text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND index_name = p_index
  ) THEN
    SET @saas_sql = CONCAT('ALTER TABLE `', p_table, '` ADD ', p_definition);
    PREPARE saas_stmt FROM @saas_sql;
    EXECUTE saas_stmt;
    DEALLOCATE PREPARE saas_stmt;
  END IF;
END$$
DELIMITER ;

CALL `saas_add_index_if_missing`('admin_user', 'idx_admin_user_type_status', 'KEY `idx_admin_user_type_status` (`admin_type`,`status`)');
CALL `saas_add_index_if_missing`('competition', 'idx_competition_organizer_status_deadline', 'KEY `idx_competition_organizer_status_deadline` (`organizer_id`,`status`,`registration_deadline`)');
CALL `saas_add_index_if_missing`('file_asset', 'idx_file_asset_organizer_owner', 'KEY `idx_file_asset_organizer_owner` (`organizer_id`,`owner_type`,`owner_id`)');
CALL `saas_add_index_if_missing`('admin_operation_log', 'idx_admin_log_organizer_time', 'KEY `idx_admin_log_organizer_time` (`organizer_id`,`create_time`,`id`)');
CALL `saas_add_index_if_missing`('admin_operation_log', 'idx_admin_log_competition_time', 'KEY `idx_admin_log_competition_time` (`competition_id`,`create_time`,`id`)');
CALL `saas_add_index_if_missing`('organizer_application', 'idx_organizer_application_portal_account', 'KEY `idx_organizer_application_portal_account` (`portal_account_id`)');
DROP PROCEDURE `saas_add_index_if_missing`;

-- 创建固定的平台企业账户和平台组织。名称使用 UTF-8 文件执行，避免 PowerShell 管道编码损坏。
INSERT INTO `enterprise_account` (`account_code`, `name`, `status`)
VALUES ('EA-PLATFORM', '啤酒事务局', 'ACTIVE')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `status` = 'ACTIVE';
SELECT `id` INTO @platform_enterprise_id
  FROM `enterprise_account` WHERE `account_code` = 'EA-PLATFORM' LIMIT 1;

INSERT INTO `organizer` (`enterprise_account_id`, `name`, `organizer_type`, `status`)
VALUES (@platform_enterprise_id, '啤酒事务局', 'PLATFORM', 'ACTIVE')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `status` = 'ACTIVE';
SELECT `id` INTO @platform_organizer_id
  FROM `organizer`
 WHERE `enterprise_account_id` = @platform_enterprise_id
   AND `organizer_type` = 'PLATFORM' LIMIT 1;

-- 历史数据均属于平台自办赛事，保持一期支付和退款口径。
UPDATE `competition`
   SET `organizer_id` = @platform_organizer_id
 WHERE `organizer_id` IS NULL;

UPDATE `admin_user`
   SET `admin_type` = COALESCE(NULLIF(`admin_type`, ''), 'PLATFORM_SUPER_ADMIN')
 WHERE `admin_type` IS NULL OR `admin_type` = '';

INSERT INTO `organizer_member` (`organizer_id`, `admin_user_id`, `status`)
SELECT @platform_organizer_id, `id`, 1
  FROM `admin_user` au
 WHERE NOT EXISTS (
       SELECT 1 FROM `organizer_member` om
        WHERE om.`organizer_id` = @platform_organizer_id
          AND om.`admin_user_id` = au.`id`
  );

UPDATE `file_asset`
   SET `organizer_id` = @platform_organizer_id
 WHERE `organizer_id` IS NULL;

UPDATE `admin_operation_log`
   SET `organizer_id` = @platform_organizer_id
 WHERE `organizer_id` IS NULL;

UPDATE `admin_operation_log` l
LEFT JOIN `competition` c
       ON l.`target_type` = 'COMPETITION'
      AND l.`target_public_id` REGEXP '^[0-9]+$'
      AND c.`id` = CAST(l.`target_public_id` AS UNSIGNED)
   SET l.`competition_id` = c.`id`,
       l.`organizer_id` = COALESCE(c.`organizer_id`, l.`organizer_id`)
 WHERE l.`target_type` = 'COMPETITION'
   AND l.`competition_id` IS NULL
   AND c.`id` IS NOT NULL;

-- 回填完成后收紧比赛归属约束，避免后续写入产生无主比赛。
ALTER TABLE `competition`
  MODIFY COLUMN `organizer_id` bigint NOT NULL COMMENT '比赛所属主办方';

SELECT 'saas organization baseline migration completed' AS migration_status;
SELECT `id`, `account_code`, `name`, `status`
  FROM `enterprise_account` WHERE `account_code` = 'EA-PLATFORM';
SELECT `id`, HEX(`name`) AS name_hex, `organizer_type`, `status`
  FROM `organizer` WHERE `organizer_type` = 'PLATFORM';
SELECT COUNT(*) AS competitions_without_organizer
  FROM `competition` WHERE `organizer_id` IS NULL;
SELECT COUNT(*) AS admin_users_without_type
  FROM `admin_user` WHERE `admin_type` IS NULL OR `admin_type` = '';
