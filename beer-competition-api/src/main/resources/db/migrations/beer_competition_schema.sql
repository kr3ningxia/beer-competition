-- MySQL dump 10.13  Distrib 8.1.0, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: beer_competition
-- ------------------------------------------------------
-- Server version	8.1.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admin_operation_log`
--

DROP TABLE IF EXISTS `admin_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_user_id` bigint NOT NULL COMMENT '后台管理员ID',
  `organizer_id` bigint DEFAULT NULL COMMENT '操作所属主办方',
  `competition_id` bigint DEFAULT NULL COMMENT '相关比赛',
  `action` varchar(64) NOT NULL COMMENT '操作动作',
  `target_type` varchar(32) NOT NULL COMMENT '操作对象类型',
  `target_public_id` varchar(64) DEFAULT NULL COMMENT '操作对象公开编号',
  `summary` varchar(255) DEFAULT NULL COMMENT '操作摘要',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_operation_log_target` (`target_type`,`target_public_id`),
  KEY `idx_admin_operation_log_admin` (`admin_user_id`,`create_time`),
  KEY `idx_admin_operation_log_time_id` (`create_time`,`id`),
  KEY `idx_admin_log_organizer_time` (`organizer_id`,`create_time`,`id`),
  KEY `idx_admin_log_competition_time` (`competition_id`,`create_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `admin_user`
--

DROP TABLE IF EXISTS `admin_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '登录用户名',
  `password` varchar(64) NOT NULL COMMENT '登录密码哈希',
  `name` varchar(64) NOT NULL COMMENT '管理员姓名',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  `admin_type` varchar(32) NOT NULL DEFAULT 'PLATFORM_SUPER_ADMIN' COMMENT '后台操作身份',
  `must_change_password` tinyint NOT NULL DEFAULT '0' COMMENT '是否必须首次改密',
  `initial_credential_issued_time` datetime DEFAULT NULL COMMENT '初始凭据发放时间',
  `must_change_username` tinyint NOT NULL DEFAULT '0' COMMENT '是否必须首次修改登录账号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_username` (`username`),
  KEY `idx_admin_user_type_status` (`admin_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台管理员账号表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `award_result`
--

DROP TABLE IF EXISTS `award_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `award_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `category_id` bigint DEFAULT NULL COMMENT '投递组别ID',
  `category_key` bigint GENERATED ALWAYS AS (coalesce(`category_id`,0)) STORED COMMENT '用于同一奖项槽位唯一约束的组别键',
  `beer_entry_id` bigint NOT NULL COMMENT '参赛酒款ID',
  `award_rule_id` bigint DEFAULT NULL COMMENT '奖项规则ID',
  `award_type` varchar(32) NOT NULL COMMENT '奖项类型',
  `award_name` varchar(64) NOT NULL COMMENT '奖项名称',
  `rank_no` int DEFAULT NULL COMMENT '名次',
  `source_round_id` bigint DEFAULT NULL COMMENT '来源轮次ID',
  `source_round_table_id` bigint DEFAULT NULL COMMENT '来源评审桌ID',
  `source_result_id` bigint DEFAULT NULL COMMENT '来源结果ID',
  `champion_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否冠军奖项',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认奖项的管理员ID',
  `confirmed_time` datetime DEFAULT NULL COMMENT '确认时间',
  `published_time` datetime DEFAULT NULL COMMENT '发布时间',
  `certificate_asset_id` bigint DEFAULT NULL COMMENT '证书文件ID',
  `certificate_uploaded_at` datetime DEFAULT NULL COMMENT '证书上传时间',
  `certificate_filename` varchar(255) DEFAULT NULL COMMENT '证书文件名',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_award_result_slot` (`competition_id`,`category_key`,`award_type`,`rank_no`,`status`),
  KEY `idx_award_result_entry` (`beer_entry_id`),
  KEY `idx_award_result_competition` (`competition_id`),
  KEY `idx_award_result_source` (`source_result_id`),
  KEY `idx_award_result_certificate_asset` (`certificate_asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛奖项结果表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `award_rule`
--

DROP TABLE IF EXISTS `award_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `award_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `category_id` bigint DEFAULT NULL COMMENT '投递组别ID',
  `category_key` bigint GENERATED ALWAYS AS (coalesce(`category_id`,0)) STORED COMMENT '用于同一奖项规则唯一约束的组别键',
  `award_type` varchar(32) NOT NULL COMMENT '奖项类型',
  `award_name` varchar(64) NOT NULL COMMENT '奖项名称',
  `rank_no` int DEFAULT NULL COMMENT '名次',
  `enabled_flag` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_award_rule_slot` (`competition_id`,`category_key`,`award_type`,`rank_no`),
  KEY `idx_award_rule_competition` (`competition_id`),
  KEY `idx_award_rule_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛奖项规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bank_transfer_payment`
--

DROP TABLE IF EXISTS `bank_transfer_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bank_transfer_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `transfer_no` varchar(64) NOT NULL COMMENT '银行转账付款单号',
  `brewery_id` bigint NOT NULL COMMENT '厂牌ID',
  `portal_account_id` bigint NOT NULL COMMENT '厂商账号ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `payment_order_id` bigint DEFAULT NULL COMMENT '聚合支付订单ID',
  `beer_entry_id` bigint DEFAULT NULL,
  `entry_payment_id` bigint DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `payer_name` varchar(128) DEFAULT NULL COMMENT '付款方名称',
  `transfer_time` datetime NOT NULL COMMENT '银行转账时间',
  `remark` varchar(255) NOT NULL COMMENT '备注',
  `voucher_asset_id` bigint DEFAULT NULL COMMENT '付款凭证文件ID',
  `status` varchar(32) NOT NULL COMMENT '付款单处理状态',
  `admin_id` bigint DEFAULT NULL COMMENT '处理付款单的管理员ID',
  `admin_note` varchar(255) DEFAULT NULL COMMENT '后台处理备注',
  `submitted_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `processed_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `collection_snapshot_json` json DEFAULT NULL COMMENT '提交时收款配置快照',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bank_transfer_no` (`transfer_no`),
  KEY `idx_bank_transfer_status` (`status`,`submitted_time`),
  KEY `idx_bank_transfer_competition` (`competition_id`,`status`),
  KEY `idx_bank_transfer_brewery` (`brewery_id`,`status`),
  KEY `idx_bank_transfer_voucher_asset` (`voucher_asset_id`),
  KEY `idx_bank_transfer_entry` (`beer_entry_id`),
  KEY `idx_bank_transfer_entry_payment` (`entry_payment_id`),
  KEY `idx_bank_transfer_payment_order` (`payment_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='银行转账付款单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `beer_coin_consumption_allocation`
--

DROP TABLE IF EXISTS `beer_coin_consumption_allocation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `beer_coin_consumption_allocation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分配明细主键',
  `ledger_id` bigint NOT NULL COMMENT '扣减流水ID',
  `lot_id` bigint NOT NULL COMMENT '到账批次ID',
  `quantity` bigint NOT NULL COMMENT '本批次扣减数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_beer_coin_allocation_ledger_lot` (`ledger_id`,`lot_id`),
  KEY `idx_beer_coin_allocation_lot` (`lot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币消费批次分配';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `beer_coin_ledger`
--

DROP TABLE IF EXISTS `beer_coin_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `beer_coin_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水主键',
  `ledger_no` varchar(64) NOT NULL COMMENT '流水号',
  `enterprise_account_id` bigint NOT NULL COMMENT '企业账户ID',
  `direction` varchar(32) NOT NULL COMMENT '方向：CREDIT/DEBIT/REVERSAL',
  `quantity` bigint NOT NULL COMMENT '流水数量',
  `business_type` varchar(64) NOT NULL COMMENT '业务类型',
  `business_id` varchar(64) DEFAULT NULL COMMENT '业务ID',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `operator_admin_id` bigint DEFAULT NULL COMMENT '操作管理员',
  `reason` varchar(500) DEFAULT NULL COMMENT '操作原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_beer_coin_ledger_no` (`ledger_no`),
  UNIQUE KEY `uk_beer_coin_ledger_idempotency` (`enterprise_account_id`,`idempotency_key`),
  KEY `idx_beer_coin_ledger_account_time` (`enterprise_account_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币账户流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `beer_coin_lot`
--

DROP TABLE IF EXISTS `beer_coin_lot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `beer_coin_lot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '到账批次主键',
  `lot_no` varchar(64) NOT NULL COMMENT '到账批次号',
  `enterprise_account_id` bigint NOT NULL COMMENT '企业账户ID',
  `purchase_order_id` bigint DEFAULT NULL COMMENT '购买订单ID',
  `total_quantity` bigint NOT NULL COMMENT '到账总数',
  `remaining_quantity` bigint NOT NULL COMMENT '剩余数量',
  `available_from` datetime NOT NULL COMMENT '可用时间',
  `expires_at` datetime NOT NULL COMMENT '失效时间',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `source_order_id` varchar(64) DEFAULT NULL COMMENT '来源业务单号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_beer_coin_lot_no` (`lot_no`),
  UNIQUE KEY `uk_beer_coin_lot_purchase_order` (`purchase_order_id`),
  KEY `idx_beer_coin_lot_account_expire` (`enterprise_account_id`,`expires_at`,`available_from`,`remaining_quantity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币到账批次';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `beer_coin_product`
--

DROP TABLE IF EXISTS `beer_coin_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `beer_coin_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '全局价格设置主键',
  `product_code` varchar(64) NOT NULL COMMENT '全局价格设置编码',
  `name` varchar(128) NOT NULL COMMENT '内部兼容名称',
  `status` varchar(32) NOT NULL DEFAULT 'INACTIVE' COMMENT '内部状态：ACTIVE/INACTIVE',
  `active_guard` tinyint GENERATED ALWAYS AS (if((`status` = _utf8mb4'ACTIVE'),1,NULL)) STORED,
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `version_no` int NOT NULL DEFAULT '1' COMMENT '方案版本号',
  `created_by_admin_id` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_beer_coin_product_code` (`product_code`),
  UNIQUE KEY `uk_beer_coin_product_active_guard` (`active_guard`),
  KEY `idx_beer_coin_product_status` (`status`,`effective_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币全局价格设置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `beer_coin_product_tier`
--

DROP TABLE IF EXISTS `beer_coin_product_tier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `beer_coin_product_tier` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '阶梯主键',
  `product_id` bigint NOT NULL COMMENT '全局价格设置ID',
  `start_quantity` bigint NOT NULL COMMENT '起始数量（含）',
  `end_quantity` bigint DEFAULT NULL COMMENT '结束数量（含），为空表示以上',
  `unit_price` decimal(10,2) NOT NULL COMMENT '单枚价格',
  `sort_order` int NOT NULL COMMENT '阶梯顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_beer_coin_product_tier_order` (`product_id`,`sort_order`),
  KEY `idx_beer_coin_product_tier_range` (`product_id`,`start_quantity`,`end_quantity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币价格阶梯';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `beer_coin_purchase_order`
--

DROP TABLE IF EXISTS `beer_coin_purchase_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `beer_coin_purchase_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '购买订单主键',
  `order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `enterprise_account_id` bigint NOT NULL COMMENT '企业账户ID',
  `product_id` bigint NOT NULL COMMENT '下单时全局价格设置ID',
  `product_snapshot_json` json NOT NULL COMMENT '下单时价格和阶梯快照',
  `quantity` bigint NOT NULL COMMENT '购买数量',
  `amount` decimal(12,2) NOT NULL COMMENT '订单金额',
  `status` varchar(32) NOT NULL COMMENT '订单状态',
  `out_trade_no` varchar(64) DEFAULT NULL COMMENT '微信商户订单号',
  `wechat_transaction_id` varchar(64) DEFAULT NULL COMMENT '微信交易号',
  `code_url` varchar(512) DEFAULT NULL COMMENT 'Native 支付二维码链接',
  `expire_time` datetime DEFAULT NULL COMMENT '支付过期时间',
  `wechat_trade_state` varchar(32) DEFAULT NULL COMMENT '微信交易状态',
  `wechat_trade_state_desc` varchar(128) DEFAULT NULL COMMENT '微信交易状态描述',
  `notify_raw_json` json DEFAULT NULL COMMENT '支付通知原始JSON',
  `paid_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_beer_coin_purchase_order_no` (`order_no`),
  UNIQUE KEY `uk_beer_coin_purchase_out_trade_no` (`out_trade_no`),
  UNIQUE KEY `uk_beer_coin_purchase_wechat_transaction_id` (`wechat_transaction_id`),
  KEY `idx_beer_coin_purchase_account_status` (`enterprise_account_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒币购买订单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `beer_entry`
--

DROP TABLE IF EXISTS `beer_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `beer_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uuid` varchar(64) NOT NULL COMMENT '参赛酒款公开编号',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `brewery_id` bigint NOT NULL COMMENT '厂牌ID',
  `registration_batch_id` bigint DEFAULT NULL COMMENT '报名批次ID',
  `category_id` bigint NOT NULL COMMENT '投递组别ID',
  `name` varchar(128) NOT NULL COMMENT '参赛酒款名称',
  `style` varchar(128) NOT NULL COMMENT '报名选择的基础风格',
  `style_config_id` bigint DEFAULT NULL COMMENT '报名时选择的比赛风格快照ID',
  `abv` decimal(4,2) NOT NULL COMMENT '酒精度ABV',
  `extra_fields_json` json DEFAULT NULL COMMENT '补充报名字段快照JSON',
  `status` varchar(32) NOT NULL COMMENT '酒款报名状态',
  `stored_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否已入库',
  `box_number` varchar(20) DEFAULT NULL COMMENT '比赛内样品箱号',
  `deleted_flag` tinyint NOT NULL DEFAULT '0' COMMENT '管理端业务删除标记',
  `deleted_time` datetime DEFAULT NULL COMMENT '管理端删除时间',
  `deleted_by_admin_id` bigint DEFAULT NULL COMMENT '执行删除的管理员ID',
  `delete_reason` varchar(500) DEFAULT NULL COMMENT '管理端删除原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_beer_entry_uuid` (`uuid`),
  KEY `idx_beer_entry_batch` (`registration_batch_id`),
  KEY `idx_beer_entry_competition_status_stored` (`competition_id`,`status`,`stored_flag`),
  KEY `idx_beer_entry_active_competition` (`deleted_flag`,`competition_id`,`status`),
  KEY `idx_beer_entry_active_brewery` (`deleted_flag`,`brewery_id`,`id`),
  KEY `idx_beer_entry_competition_box` (`competition_id`,`box_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参赛酒款报名表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `beer_entry_extra_field`
--

DROP TABLE IF EXISTS `beer_entry_extra_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `beer_entry_extra_field` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `beer_entry_id` bigint NOT NULL COMMENT '参赛酒款ID',
  `field_key` varchar(64) NOT NULL COMMENT '补充字段Key',
  `field_label` varchar(64) NOT NULL COMMENT '补充字段名称快照',
  `field_value` varchar(255) NOT NULL COMMENT '补充字段填写值',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参赛酒款补充字段值表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `brewery`
--

DROP TABLE IF EXISTS `brewery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brewery` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `company_name` varchar(128) NOT NULL COMMENT '厂牌或公司名称',
  `contact_name` varchar(64) NOT NULL COMMENT '联系人姓名',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `wechat` varchar(64) DEFAULT NULL COMMENT '微信号',
  `avatar_asset_id` bigint DEFAULT NULL COMMENT '头像文件ID',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '厂牌头像访问地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='厂牌资料表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition`
--

DROP TABLE IF EXISTS `competition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `organizer_id` bigint NOT NULL COMMENT '比赛所属主办方',
  `code` varchar(64) NOT NULL COMMENT '比赛编码',
  `name` varchar(128) NOT NULL COMMENT '比赛名称',
  `competition_date` date NOT NULL COMMENT '比赛日期',
  `registration_start` datetime DEFAULT NULL COMMENT '报名开始时间',
  `registration_deadline` datetime NOT NULL COMMENT '报名截止时间',
  `status` varchar(32) NOT NULL COMMENT '比赛流程状态',
  `competition_type` varchar(32) NOT NULL DEFAULT 'AWARD' COMMENT '比赛类型',
  `entry_fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '标准报名费',
  `early_bird_fee` decimal(10,2) DEFAULT NULL COMMENT '早鸟报名费',
  `early_bird_deadline` datetime DEFAULT NULL COMMENT '早鸟价截止时间',
  `tier_pricing_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用累计阶梯报名价',
  `refund_approval_mode` varchar(32) NOT NULL DEFAULT 'AUTO_APPROVE',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '说明',
  `rules_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '参赛规则文件地址',
  `style_library_version` varchar(64) NOT NULL DEFAULT 'BJCP_2021_CN' COMMENT '绑定风格库版本',
  `delivery_method` varchar(32) NOT NULL DEFAULT 'BOTH' COMMENT '支持的送样方式',
  `sample_arrival_start` datetime DEFAULT NULL COMMENT '收样开始时间',
  `sample_arrival_deadline` datetime DEFAULT NULL COMMENT '收样截止时间',
  `sample_quantity_note` varchar(255) DEFAULT NULL COMMENT '送样数量说明',
  `delivery_recipient` varchar(64) DEFAULT NULL COMMENT '收样联系人',
  `delivery_phone` varchar(64) DEFAULT NULL COMMENT '收样联系电话',
  `delivery_address` varchar(500) DEFAULT NULL COMMENT '收样地址',
  `delivery_note` varchar(1000) DEFAULT NULL COMMENT '送样补充说明',
  `logistics_visibility` varchar(32) NOT NULL DEFAULT 'PAYMENT_CONFIRMED' COMMENT '送样信息可见范围',
  `deleted_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_code` (`code`),
  KEY `idx_competition_status_registration_deadline` (`status`,`registration_deadline`),
  KEY `idx_competition_organizer_status_deadline` (`organizer_id`,`status`,`registration_deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_category`
--

DROP TABLE IF EXISTS `competition_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `name` varchar(128) NOT NULL COMMENT '投递组别名称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_category_name` (`competition_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛投递组别配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_coin_settlement`
--

DROP TABLE IF EXISTS `competition_coin_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_coin_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '赛事结算主键',
  `competition_id` bigint NOT NULL COMMENT '比赛ID',
  `enterprise_account_id` bigint NOT NULL COMMENT '企业账户ID',
  `settlement_type` varchar(32) NOT NULL COMMENT '结算类型',
  `effective_entry_count` int NOT NULL DEFAULT '0' COMMENT '有效酒款数',
  `billing_tier` varchar(128) DEFAULT NULL COMMENT '计费说明',
  `required_quantity` bigint NOT NULL DEFAULT '0' COMMENT '应消耗数量',
  `charged_quantity` bigint NOT NULL DEFAULT '0' COMMENT '本次扣除数量',
  `status` varchar(32) NOT NULL COMMENT '结算状态',
  `ledger_id` bigint DEFAULT NULL COMMENT '扣减流水ID',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `settled_by_admin_id` bigint DEFAULT NULL COMMENT '操作管理员',
  `settled_time` datetime DEFAULT NULL COMMENT '结算时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_coin_settlement_type` (`competition_id`,`settlement_type`),
  UNIQUE KEY `uk_competition_coin_settlement_idempotency` (`enterprise_account_id`,`idempotency_key`),
  KEY `idx_competition_coin_settlement_account` (`enterprise_account_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='赛事啤酒币结算';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_coin_settlement_entry`
--

DROP TABLE IF EXISTS `competition_coin_settlement_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_coin_settlement_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '结算酒款快照主键',
  `settlement_id` bigint NOT NULL COMMENT '结算ID',
  `beer_entry_id` bigint NOT NULL COMMENT '酒款ID',
  `entry_status_snapshot` varchar(32) NOT NULL COMMENT '结算时酒款状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_coin_settlement_entry` (`settlement_id`,`beer_entry_id`),
  KEY `idx_competition_coin_settlement_entry_entry` (`beer_entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='赛事啤酒币结算酒款快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_collection_config`
--

DROP TABLE IF EXISTS `competition_collection_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_collection_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '租户赛事ID',
  `wechat_qr_asset_id` bigint DEFAULT NULL COMMENT '微信收款码文件资产ID',
  `bank_account_name` varchar(128) DEFAULT NULL COMMENT '收款账户名',
  `bank_account_no` varchar(128) DEFAULT NULL COMMENT '收款账号',
  `bank_name` varchar(128) DEFAULT NULL COMMENT '开户行',
  `collection_note` varchar(1000) DEFAULT NULL COMMENT '收款提示',
  `payment_contact` varchar(128) DEFAULT NULL COMMENT '付款咨询联系（微信/电话）',
  `enabled_methods_json` varchar(255) NOT NULL COMMENT '启用收款方式 JSON 数组',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_competition` (`competition_id`),
  KEY `idx_collection_qr_asset` (`wechat_qr_asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户赛事收款配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_fee_tier`
--

DROP TABLE IF EXISTS `competition_fee_tier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_fee_tier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `start_quantity` int NOT NULL COMMENT '从第几款起生效',
  `discount_rate` decimal(8,4) NOT NULL COMMENT '折扣率，1.0000 为原价',
  `sort_order` int NOT NULL DEFAULT '0',
  `enabled` tinyint NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_fee_tier_start` (`competition_id`,`start_quantity`),
  KEY `idx_competition_fee_tier_competition` (`competition_id`,`enabled`,`start_quantity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛累计阶梯报名价';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_judge_assignment`
--

DROP TABLE IF EXISTS `competition_judge_assignment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_judge_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `base_table_id` bigint NOT NULL COMMENT '基础评审桌ID',
  `judge_account_id` bigint NOT NULL COMMENT '评委账号ID',
  `role` varchar(32) NOT NULL COMMENT '评委在基础桌的角色',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `recruitment_application_id` bigint DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '本场分配状态 ACTIVE/WITHDRAWN',
  `withdrawn_time` datetime DEFAULT NULL,
  `withdrawn_by` bigint DEFAULT NULL,
  `withdraw_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_judge_assignment` (`competition_id`,`judge_account_id`),
  KEY `idx_competition_judge_assignment_table` (`base_table_id`),
  KEY `idx_judge_assignment_recruitment_application` (`recruitment_application_id`),
  KEY `idx_judge_assignment_competition_status` (`competition_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛基础评审桌评委分配表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_judge_evaluation`
--

DROP TABLE IF EXISTS `competition_judge_evaluation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_judge_evaluation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `judge_account_id` bigint NOT NULL,
  `judgment_level` tinyint DEFAULT NULL,
  `feedback_quality_level` tinyint DEFAULT NULL,
  `rule_execution_level` tinyint DEFAULT NULL,
  `professionalism_level` tinyint DEFAULT NULL,
  `manual_score` decimal(5,1) DEFAULT NULL,
  `comment_total_chars` int NOT NULL DEFAULT '0',
  `comment_average_chars` int NOT NULL DEFAULT '0',
  `comment_record_count` int NOT NULL DEFAULT '0',
  `comment_requirement_ratio` decimal(8,3) NOT NULL DEFAULT '0.000',
  `comment_percentile` decimal(6,2) DEFAULT NULL,
  `comment_score` decimal(5,1) DEFAULT NULL,
  `task_completed_count` int NOT NULL DEFAULT '0',
  `task_total_count` int NOT NULL DEFAULT '0',
  `completion_rate` decimal(6,2) NOT NULL DEFAULT '0.00',
  `total_score` decimal(5,1) DEFAULT NULL,
  `excellent_candidate` tinyint NOT NULL DEFAULT '0',
  `evidence` varchar(1000) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT',
  `evaluated_by` bigint DEFAULT NULL,
  `evaluated_time` datetime DEFAULT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `confirmed_time` datetime DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_judge_evaluation` (`competition_id`,`judge_account_id`),
  KEY `idx_judge_evaluation_judge` (`judge_account_id`,`status`),
  KEY `idx_judge_evaluation_competition_score` (`competition_id`,`status`,`total_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛级评审表现评价';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_judge_public_profile`
--

DROP TABLE IF EXISTS `competition_judge_public_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_judge_public_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '比赛ID',
  `judge_account_id` bigint NOT NULL COMMENT '评委账号ID',
  `name` varchar(64) NOT NULL COMMENT '发布时公开姓名',
  `qualification` varchar(255) DEFAULT NULL COMMENT '发布时公开资质',
  `avatar_asset_id` bigint DEFAULT NULL COMMENT '发布时头像文件资产ID',
  `roles_json` varchar(1024) NOT NULL COMMENT '发布时公开角色JSON',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '公开展示顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_judge_public_profile` (`competition_id`,`judge_account_id`),
  KEY `idx_competition_judge_public_profile_avatar` (`avatar_asset_id`),
  KEY `idx_competition_judge_public_profile_competition` (`competition_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='赛事公开评委资料快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_judge_table`
--

DROP TABLE IF EXISTS `competition_judge_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_judge_table` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `table_name` varchar(64) NOT NULL COMMENT '基础评审桌名称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_judge_table_name` (`competition_id`,`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛基础评审桌配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_notification_rule`
--

DROP TABLE IF EXISTS `competition_notification_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_notification_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '比赛ID',
  `event_code` varchar(64) NOT NULL COMMENT '通知事件',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用',
  `schedule_mode` varchar(32) NOT NULL COMMENT '发送方式',
  `scheduled_at` datetime DEFAULT NULL COMMENT '固定发送时间',
  `offset_minutes` int DEFAULT NULL COMMENT '相对节点分钟数',
  `send_time` varchar(5) NOT NULL DEFAULT '10:00' COMMENT '相对节点发送时刻',
  `timezone` varchar(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '时区',
  `template_id` bigint DEFAULT NULL COMMENT '模板版本ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_notification_rule` (`competition_id`,`event_code`),
  KEY `idx_competition_notification_rule_due` (`enabled`,`schedule_mode`,`scheduled_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛邮件通知规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_round`
--

DROP TABLE IF EXISTS `competition_round`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_round` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `round_no` int NOT NULL COMMENT '轮次序号',
  `round_name` varchar(64) NOT NULL COMMENT '轮次名称',
  `round_type` varchar(32) NOT NULL COMMENT '轮次类型',
  `source_round_id` bigint DEFAULT NULL COMMENT '来源轮次ID',
  `status` varchar(32) NOT NULL COMMENT '轮次状态',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `published_time` datetime DEFAULT NULL COMMENT '发布时间',
  `submitted_time` datetime DEFAULT NULL COMMENT '提交时间',
  `locked_time` datetime DEFAULT NULL COMMENT '锁定时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `allocation_revision` bigint NOT NULL DEFAULT '0' COMMENT '轮次编排修订号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_round_no` (`competition_id`,`round_no`),
  KEY `idx_competition_round_source` (`source_round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛评审轮次表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_score_config`
--

DROP TABLE IF EXISTS `competition_score_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_score_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `judge_role_type` varchar(32) NOT NULL COMMENT '评委角色类型',
  `min_comment_length` int NOT NULL DEFAULT '0' COMMENT '评语最小字数',
  `dimensions_json` json NOT NULL COMMENT '评分维度JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_score_config` (`competition_id`,`judge_role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛评分表配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_sponsor`
--

DROP TABLE IF EXISTS `competition_sponsor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_sponsor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `tier_label` varchar(32) NOT NULL COMMENT '赞助等级',
  `sponsor_name` varchar(64) NOT NULL COMMENT '赞助商名称',
  `logo_asset_id` bigint DEFAULT NULL COMMENT 'Logo 文件资产ID',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `featured_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否重点展示',
  `enabled_flag` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_competition_sponsor_competition` (`competition_id`,`enabled_flag`,`sort_order`),
  KEY `idx_competition_sponsor_logo` (`logo_asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛赞助商配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competition_style_config`
--

DROP TABLE IF EXISTS `competition_style_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `competition_style_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `name` varchar(128) NOT NULL COMMENT '风格名称',
  `category_name` varchar(128) DEFAULT NULL COMMENT '风格分类名称',
  `style_code` varchar(64) DEFAULT NULL COMMENT '风格编码',
  `description` text COMMENT '说明',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `active_flag` tinyint NOT NULL DEFAULT '1' COMMENT '是否为当前可报名风格',
  `source_library_version` varchar(64) DEFAULT NULL COMMENT '来源风格库版本',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_competition_style_active` (`competition_id`,`active_flag`,`sort_order`),
  KEY `idx_competition_style_name` (`competition_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛可选基础风格配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `email_delivery`
--

DROP TABLE IF EXISTS `email_delivery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `email_delivery` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '比赛ID',
  `event_code` varchar(64) NOT NULL COMMENT '通知事件',
  `recipient_account_id` bigint NOT NULL COMMENT '厂商账号ID',
  `recipient_email_enc` varchar(512) NOT NULL COMMENT '收件邮箱加密值',
  `recipient_email_hash` varchar(64) NOT NULL COMMENT '收件邮箱摘要',
  `result_version` int NOT NULL DEFAULT '1' COMMENT '结果版本',
  `subject_snapshot` varchar(200) NOT NULL COMMENT '主题快照',
  `html_body_snapshot` mediumtext NOT NULL COMMENT '正文快照',
  `scheduled_time` datetime NOT NULL COMMENT '计划发送时间',
  `status` varchar(32) NOT NULL COMMENT '发送状态',
  `attempt_count` int NOT NULL DEFAULT '0' COMMENT '尝试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `provider_request_id` varchar(128) DEFAULT NULL COMMENT '供应商请求编号',
  `last_error` varchar(1000) DEFAULT NULL COMMENT '最近错误',
  `sent_time` datetime DEFAULT NULL COMMENT '发送时间',
  `delivered_time` datetime DEFAULT NULL COMMENT '投递时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email_delivery_dedup` (`competition_id`,`event_code`,`recipient_account_id`,`result_version`),
  KEY `idx_email_delivery_pending` (`status`,`scheduled_time`,`next_retry_time`),
  KEY `idx_email_delivery_competition` (`competition_id`,`status`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件发送队列与日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `enterprise_account`
--

DROP TABLE IF EXISTS `enterprise_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enterprise_account` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `entry_delivery`
--

DROP TABLE IF EXISTS `entry_delivery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entry_delivery` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `beer_entry_id` bigint NOT NULL COMMENT '参赛酒款ID',
  `delivery_method` varchar(32) DEFAULT NULL COMMENT '送样方式',
  `carrier` varchar(64) DEFAULT NULL COMMENT '快递公司',
  `tracking_no` varchar(64) DEFAULT NULL COMMENT '快递单号',
  `delivery_note` varchar(500) DEFAULT NULL COMMENT '厂商送样备注',
  `delivery_status` varchar(32) NOT NULL COMMENT '送样状态',
  `submitted_time` datetime DEFAULT NULL COMMENT '提交时间',
  `received_time` datetime DEFAULT NULL COMMENT '收样确认时间',
  `received_by_admin_id` bigint DEFAULT NULL COMMENT '收样确认管理员ID',
  `receive_remark` varchar(255) DEFAULT NULL COMMENT '收样确认备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_delivery_entry` (`beer_entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参赛酒款送样记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `entry_field_config`
--

DROP TABLE IF EXISTS `entry_field_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entry_field_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `field_key` varchar(64) NOT NULL COMMENT '字段Key',
  `field_label` varchar(64) NOT NULL COMMENT '字段名称',
  `field_type` varchar(32) NOT NULL COMMENT '字段类型',
  `help_text` varchar(255) DEFAULT NULL COMMENT '填写提示文案',
  `options_json` json DEFAULT NULL COMMENT '选项JSON',
  `required_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否必填',
  `visible_to_judges` tinyint NOT NULL DEFAULT '0' COMMENT '是否对评审可见',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `active_flag` tinyint NOT NULL DEFAULT '1' COMMENT '是否为当前报名字段',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_field_config_key` (`competition_id`,`field_key`),
  KEY `idx_entry_field_active` (`competition_id`,`active_flag`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛报名补充字段配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `entry_payment`
--

DROP TABLE IF EXISTS `entry_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entry_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `beer_entry_id` bigint NOT NULL COMMENT '参赛酒款ID',
  `payment_order_id` bigint DEFAULT NULL COMMENT '聚合支付订单ID',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `status` varchar(32) NOT NULL COMMENT '支付状态',
  `pay_method` varchar(32) NOT NULL COMMENT '支付方式',
  `out_trade_no` varchar(64) DEFAULT NULL COMMENT '商户支付单号',
  `wechat_transaction_id` varchar(64) DEFAULT NULL COMMENT '微信支付交易号',
  `bank_transfer_id` bigint DEFAULT NULL COMMENT '关联银行转账付款单ID',
  `code_url` varchar(512) DEFAULT NULL COMMENT '微信Native支付二维码链接',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `paid_amount` decimal(10,2) DEFAULT NULL COMMENT '实付金额',
  `wechat_trade_state` varchar(32) DEFAULT NULL COMMENT '微信交易状态',
  `wechat_trade_state_desc` varchar(128) DEFAULT NULL COMMENT '微信交易状态描述',
  `notify_raw_json` json DEFAULT NULL COMMENT '支付通知原始JSON',
  `last_query_time` datetime DEFAULT NULL COMMENT '最近查询时间',
  `paid_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `confirmed_by_admin_id` bigint DEFAULT NULL COMMENT '人工确认管理员ID',
  `confirm_remark` varchar(255) DEFAULT NULL COMMENT '人工确认备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `pricing_base_amount` decimal(10,2) DEFAULT NULL COMMENT '计价基础单价快照',
  `discount_rate` decimal(8,4) DEFAULT NULL COMMENT '成交折扣率快照',
  `pricing_sequence` int DEFAULT NULL COMMENT '本场本厂牌累计序号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_payment_entry` (`beer_entry_id`),
  UNIQUE KEY `uk_entry_payment_out_trade_no` (`out_trade_no`),
  KEY `idx_entry_payment_status` (`status`,`update_time`),
  KEY `idx_entry_payment_bank_transfer` (`bank_transfer_id`),
  KEY `idx_entry_payment_order` (`payment_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参赛酒款报名支付记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `entry_refund`
--

DROP TABLE IF EXISTS `entry_refund`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entry_refund` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `beer_entry_id` bigint NOT NULL COMMENT '参赛酒款ID',
  `entry_payment_id` bigint NOT NULL COMMENT '报名支付记录ID',
  `payment_order_item_id` bigint DEFAULT NULL COMMENT '聚合订单酒款明细ID',
  `refund_no` varchar(64) NOT NULL COMMENT '系统退款单号',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `status` varchar(32) NOT NULL COMMENT '退款状态',
  `approval_mode_snapshot` varchar(32) DEFAULT NULL,
  `offline_refund_account_name` varchar(128) DEFAULT NULL,
  `offline_refund_bank_name` varchar(128) DEFAULT NULL,
  `offline_refund_account_no_enc` varchar(512) DEFAULT NULL,
  `offline_refund_account_no_last4` varchar(8) DEFAULT NULL,
  `offline_refund_transfer_no` varchar(128) DEFAULT NULL,
  `offline_refund_time` datetime DEFAULT NULL,
  `offline_refund_voucher_asset_id` bigint DEFAULT NULL,
  `reason` varchar(300) NOT NULL COMMENT '退款申请原因',
  `requested_by_portal_id` bigint DEFAULT NULL COMMENT '申请退款的厂商账号ID',
  `requested_time` datetime NOT NULL COMMENT '退款申请时间',
  `processed_by_admin_id` bigint DEFAULT NULL COMMENT '处理退款的管理员ID',
  `processed_time` datetime DEFAULT NULL COMMENT '处理时间',
  `success_time` datetime DEFAULT NULL COMMENT '退款成功时间',
  `fail_reason` varchar(300) DEFAULT NULL COMMENT '退款失败原因',
  `wechat_refund_id` varchar(64) DEFAULT NULL COMMENT '微信退款单号',
  `wechat_refund_status` varchar(32) DEFAULT NULL COMMENT '微信退款状态',
  `out_refund_no` varchar(64) DEFAULT NULL COMMENT '商户退款单号',
  `notify_raw_json` json DEFAULT NULL COMMENT '支付通知原始JSON',
  `refund_notify_raw_json` json DEFAULT NULL COMMENT '退款通知原始JSON',
  `last_query_time` datetime DEFAULT NULL COMMENT '最近查询时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_refund_no` (`refund_no`),
  UNIQUE KEY `uk_entry_refund_out_refund_no` (`out_refund_no`),
  KEY `idx_entry_refund_entry` (`beer_entry_id`),
  KEY `idx_entry_refund_payment` (`entry_payment_id`),
  KEY `idx_entry_refund_status` (`status`,`requested_time`),
  KEY `idx_entry_refund_order_item` (`payment_order_item_id`),
  KEY `idx_entry_refund_entry_latest` (`beer_entry_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参赛酒款退款记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `entry_scan_label`
--

DROP TABLE IF EXISTS `entry_scan_label`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entry_scan_label` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `beer_entry_id` bigint NOT NULL COMMENT '参赛酒款ID',
  `label_code` varchar(64) NOT NULL COMMENT '完整标签编码',
  `short_code` varchar(16) NOT NULL COMMENT '评审短编号',
  `scan_token` varchar(64) NOT NULL COMMENT '扫码识别Token',
  `status` varchar(32) NOT NULL COMMENT '标签状态',
  `generated_by` bigint DEFAULT NULL COMMENT '生成标签的管理员ID',
  `generated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `printed_time` datetime DEFAULT NULL COMMENT '打印时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `active_flag` tinyint GENERATED ALWAYS AS ((case when (`status` = _utf8mb4'ACTIVE') then 1 else NULL end)) VIRTUAL COMMENT '有效标签虚拟标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_scan_label_label_code` (`label_code`),
  UNIQUE KEY `uk_entry_scan_label_short_code` (`short_code`),
  UNIQUE KEY `uk_entry_scan_label_token` (`scan_token`),
  UNIQUE KEY `uk_entry_scan_label_active_entry` (`beer_entry_id`,`active_flag`),
  KEY `idx_entry_scan_label_competition` (`competition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参赛酒款扫码标签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `file_asset`
--

DROP TABLE IF EXISTS `file_asset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `organizer_id` bigint DEFAULT NULL COMMENT '文件所属主办方',
  `business_type` varchar(64) NOT NULL COMMENT '文件业务类型',
  `owner_type` varchar(32) DEFAULT NULL COMMENT '归属对象类型',
  `owner_id` bigint DEFAULT NULL COMMENT '归属对象ID',
  `storage_provider` varchar(32) NOT NULL COMMENT '存储服务提供方',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `storage_path` varchar(255) NOT NULL COMMENT '存储路径',
  `public_url` varchar(500) DEFAULT NULL COMMENT '公开访问地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_asset_owner` (`owner_type`,`owner_id`),
  KEY `idx_file_asset_organizer_owner` (`organizer_id`,`owner_type`,`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件资产表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `judge_account`
--

DROP TABLE IF EXISTS `judge_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `judge_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `public_id` varchar(32) NOT NULL COMMENT '评委公开编号',
  `phone_enc` text NOT NULL COMMENT '手机号密文',
  `phone_hash` char(64) NOT NULL COMMENT '手机号哈希值',
  `phone_last4` varchar(4) DEFAULT NULL COMMENT '手机号后四位',
  `wechat_enc` text COMMENT '微信号密文',
  `name` varchar(64) NOT NULL COMMENT '评委姓名',
  `qualification` varchar(255) NOT NULL COMMENT '评委资质说明',
  `bjcp_number` varchar(64) DEFAULT NULL COMMENT 'BJCP编号',
  `brewery_conflict_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否存在厂牌利益关系',
  `brewery_conflict_text` varchar(500) DEFAULT NULL COMMENT '利益关系说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '评委账号状态',
  `submitted_time` datetime DEFAULT NULL COMMENT '提交时间',
  `reviewed_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reviewed_by` bigint DEFAULT NULL COMMENT '审核管理员ID',
  `review_remark` varchar(255) DEFAULT NULL COMMENT '审核备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `avatar_asset_id` bigint DEFAULT NULL COMMENT '评委头像文件资产ID',
  `public_profile_consent` tinyint NOT NULL DEFAULT '0' COMMENT '是否同意赛事结果公开评委资料',
  `public_profile_consent_time` datetime DEFAULT NULL COMMENT '公开评委资料授权时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_judge_account_public_id` (`public_id`),
  UNIQUE KEY `uk_judge_account_phone_hash` (`phone_hash`),
  KEY `idx_judge_account_phone_last4` (`phone_last4`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评委账号资料表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `judge_recruitment`
--

DROP TABLE IF EXISTS `judge_recruitment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `judge_recruitment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `public_id` varchar(64) NOT NULL,
  `competition_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT',
  `recruitment_start` datetime NOT NULL,
  `recruitment_deadline` datetime NOT NULL,
  `venue` varchar(255) NOT NULL,
  `address` varchar(500) DEFAULT NULL,
  `description` text,
  `requirements` text,
  `expected_count` int DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `closed_time` datetime DEFAULT NULL,
  `reopened_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_judge_recruitment_public_id` (`public_id`),
  UNIQUE KEY `uk_judge_recruitment_competition` (`competition_id`),
  KEY `idx_judge_recruitment_status_window` (`status`,`recruitment_start`,`recruitment_deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `judge_recruitment_application`
--

DROP TABLE IF EXISTS `judge_recruitment_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `judge_recruitment_application` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `public_id` varchar(64) NOT NULL,
  `recruitment_id` bigint NOT NULL,
  `judge_account_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'APPLIED',
  `availability_confirmed` tinyint NOT NULL DEFAULT '0',
  `note` varchar(1000) DEFAULT NULL,
  `review_remark` varchar(1000) DEFAULT NULL,
  `processed_by` bigint DEFAULT NULL,
  `processed_time` datetime DEFAULT NULL,
  `withdrawn_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_judge_recruitment_application_public_id` (`public_id`),
  UNIQUE KEY `uk_judge_recruitment_application_judge` (`recruitment_id`,`judge_account_id`),
  KEY `idx_judge_recruitment_application_status` (`recruitment_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `judge_score_session`
--

DROP TABLE IF EXISTS `judge_score_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `judge_score_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `competition_id` bigint NOT NULL,
  `round_id` bigint NOT NULL,
  `round_table_id` bigint NOT NULL,
  `beer_entry_id` bigint NOT NULL,
  `judge_account_id` bigint NOT NULL,
  `judge_role_type` varchar(32) NOT NULL,
  `started_at` datetime DEFAULT NULL,
  `first_submitted_at` datetime DEFAULT NULL,
  `last_submitted_at` datetime DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `comment_char_count` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_judge_score_session_round_entry_judge_role` (`round_table_id`,`beer_entry_id`,`judge_account_id`,`judge_role_type`),
  KEY `idx_judge_score_session_round` (`round_id`),
  KEY `idx_judge_score_session_judge` (`judge_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评审评分过程会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_template`
--

DROP TABLE IF EXISTS `notification_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_code` varchar(64) NOT NULL COMMENT '通知事件',
  `name` varchar(128) NOT NULL COMMENT '模板名称',
  `version` int NOT NULL COMMENT '模板版本',
  `subject` varchar(200) NOT NULL COMMENT '邮件主题',
  `html_body` mediumtext NOT NULL COMMENT 'HTML正文',
  `status` varchar(32) NOT NULL COMMENT '模板状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建管理员ID',
  `published_time` datetime DEFAULT NULL COMMENT '启用时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_template_event_version` (`event_code`,`version`),
  KEY `idx_notification_template_event_status` (`event_code`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件通知模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organizer`
--

DROP TABLE IF EXISTS `organizer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organizer` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organizer_application`
--

DROP TABLE IF EXISTS `organizer_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organizer_application` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organizer_member`
--

DROP TABLE IF EXISTS `organizer_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organizer_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员关系主键',
  `organizer_id` bigint NOT NULL COMMENT '主办方组织',
  `admin_user_id` bigint NOT NULL COMMENT '后台账号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 启用 0 停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_organizer_member` (`organizer_id`,`admin_user_id`),
  UNIQUE KEY `uk_organizer_member_admin_user` (`admin_user_id`),
  KEY `idx_organizer_member_admin` (`admin_user_id`,`status`),
  KEY `idx_organizer_member_organizer` (`organizer_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='主办方管理员成员关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_order`
--

DROP TABLE IF EXISTS `payment_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '系统支付订单号',
  `registration_batch_id` bigint NOT NULL COMMENT '报名批次ID',
  `amount` decimal(10,2) NOT NULL COMMENT '订单应付总额',
  `paid_amount` decimal(10,2) DEFAULT NULL COMMENT '实付金额',
  `refunded_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '累计退款金额',
  `status` varchar(32) NOT NULL COMMENT '订单状态',
  `pay_method` varchar(32) NOT NULL COMMENT '支付方式',
  `out_trade_no` varchar(64) DEFAULT NULL COMMENT '微信商户支付单号',
  `wechat_transaction_id` varchar(64) DEFAULT NULL COMMENT '微信支付交易号',
  `bank_transfer_id` bigint DEFAULT NULL COMMENT '银行转账付款单ID',
  `code_url` varchar(512) DEFAULT NULL COMMENT '微信Native支付二维码链接',
  `expire_time` datetime DEFAULT NULL COMMENT '订单过期时间',
  `wechat_trade_state` varchar(32) DEFAULT NULL COMMENT '微信交易状态',
  `wechat_trade_state_desc` varchar(128) DEFAULT NULL COMMENT '微信交易状态描述',
  `notify_raw_json` json DEFAULT NULL COMMENT '支付通知原始JSON',
  `last_query_time` datetime DEFAULT NULL COMMENT '最近查询时间',
  `paid_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `manual_submit_time` datetime DEFAULT NULL COMMENT '主办方收款码付款申报时间',
  `manual_confirmed_by_admin_id` bigint DEFAULT NULL COMMENT '主办方收款确认人',
  `manual_confirmed_time` datetime DEFAULT NULL COMMENT '主办方收款确认时间',
  `manual_confirm_remark` varchar(255) DEFAULT NULL COMMENT '主办方收款审核备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_order_no` (`order_no`),
  UNIQUE KEY `uk_payment_order_batch` (`registration_batch_id`),
  UNIQUE KEY `uk_payment_order_out_trade_no` (`out_trade_no`),
  KEY `idx_payment_order_status` (`status`,`update_time`),
  KEY `idx_payment_order_bank_transfer` (`bank_transfer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聚合报名支付订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_order_item`
--

DROP TABLE IF EXISTS `payment_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `payment_order_id` bigint NOT NULL COMMENT '聚合支付订单ID',
  `beer_entry_id` bigint NOT NULL COMMENT '参赛酒款ID',
  `entry_payment_id` bigint NOT NULL COMMENT '单款应收记录ID',
  `amount` decimal(10,2) NOT NULL COMMENT '酒款分摊金额',
  `refunded_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '酒款累计退款金额',
  `status` varchar(32) NOT NULL COMMENT '分项状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `pricing_base_amount` decimal(10,2) DEFAULT NULL COMMENT '计价基础单价快照',
  `discount_rate` decimal(8,4) DEFAULT NULL COMMENT '成交折扣率快照',
  `pricing_sequence` int DEFAULT NULL COMMENT '本场本厂牌累计序号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_order_item_entry` (`beer_entry_id`),
  UNIQUE KEY `uk_payment_order_item_payment` (`entry_payment_id`),
  KEY `idx_payment_order_item_order` (`payment_order_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聚合支付订单酒款明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `portal_account`
--

DROP TABLE IF EXISTS `portal_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `wechat` varchar(64) DEFAULT NULL COMMENT '微信号',
  `display_name` varchar(64) NOT NULL COMMENT '显示名称',
  `brewery_id` bigint NOT NULL COMMENT '厂牌ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `email_enc` varchar(512) DEFAULT NULL COMMENT '邮箱加密值',
  `email_hash` varchar(64) DEFAULT NULL COMMENT '邮箱摘要',
  `email_last4` varchar(8) DEFAULT NULL COMMENT '邮箱本地部分末4位',
  `email_bounce_status` varchar(32) DEFAULT NULL COMMENT '邮箱退信状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_portal_account_phone` (`phone`),
  UNIQUE KEY `uk_portal_account_email_hash` (`email_hash`),
  KEY `idx_portal_account_brewery` (`brewery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='厂商端账号表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `registration_batch`
--

DROP TABLE IF EXISTS `registration_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registration_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_no` varchar(64) NOT NULL COMMENT '报名批次号',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `brewery_id` bigint NOT NULL COMMENT '厂牌ID',
  `portal_account_id` bigint NOT NULL COMMENT '提交账号ID',
  `entry_count` int NOT NULL COMMENT '酒款数量',
  `total_amount` decimal(10,2) NOT NULL COMMENT '批次应付总额',
  `status` varchar(32) NOT NULL COMMENT '批次状态',
  `idempotency_key` varchar(64) NOT NULL COMMENT '客户端幂等键',
  `rules_accepted` tinyint NOT NULL DEFAULT '0' COMMENT '是否同意参赛细则',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_registration_batch_no` (`batch_no`),
  UNIQUE KEY `uk_registration_batch_idempotency` (`portal_account_id`,`idempotency_key`),
  KEY `idx_registration_batch_brewery` (`brewery_id`,`create_time`),
  KEY `idx_registration_batch_competition` (`competition_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多酒款报名批次表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `round_judge_ranking_draft`
--

DROP TABLE IF EXISTS `round_judge_ranking_draft`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `round_judge_ranking_draft` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `round_id` bigint NOT NULL COMMENT '评审轮次ID',
  `round_table_id` bigint NOT NULL COMMENT '评审桌ID',
  `judge_account_id` bigint NOT NULL COMMENT '评委账号ID',
  `rankings_json` json NOT NULL COMMENT '评委个人排序草稿JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_judge_ranking_draft` (`round_table_id`,`judge_account_id`),
  KEY `idx_round_judge_ranking_draft_round` (`round_id`),
  KEY `idx_round_judge_ranking_draft_judge` (`judge_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='排序轮评委个人排序草稿表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `round_result`
--

DROP TABLE IF EXISTS `round_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `round_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `round_id` bigint NOT NULL COMMENT '评审轮次ID',
  `round_table_id` bigint NOT NULL COMMENT '评审桌ID',
  `beer_entry_id` bigint NOT NULL COMMENT '参赛酒款ID',
  `result_type` varchar(32) NOT NULL COMMENT '结果类型',
  `rank_no` int DEFAULT NULL COMMENT '名次',
  `slot_label` varchar(64) DEFAULT NULL COMMENT '结果槽位名称',
  `submitted_by` bigint DEFAULT NULL COMMENT '提交结果的管理员或桌长ID',
  `submitted_time` datetime DEFAULT NULL COMMENT '提交时间',
  `locked_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否锁定',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_result_slot` (`round_table_id`,`result_type`,`rank_no`),
  KEY `idx_round_result_entry` (`beer_entry_id`),
  KEY `idx_round_result_competition` (`competition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评审轮次结果表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `round_table`
--

DROP TABLE IF EXISTS `round_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `round_table` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `round_id` bigint NOT NULL COMMENT '评审轮次ID',
  `table_name` varchar(64) NOT NULL COMMENT '评审桌名称',
  `captain_judge_id` bigint DEFAULT NULL COMMENT '桌长评委ID',
  `category_id` bigint DEFAULT NULL COMMENT '投递组别ID',
  `category_mode` varchar(32) NOT NULL DEFAULT 'EMPTY' COMMENT '组别绑定方式',
  `target_count` int NOT NULL DEFAULT '1' COMMENT '目标晋级或获奖数量',
  `target_mode` varchar(32) NOT NULL COMMENT '目标产生方式',
  `status` varchar(32) NOT NULL COMMENT '评审桌状态',
  `result_version` int NOT NULL DEFAULT '0' COMMENT '结果版本号',
  `confirmation_override_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否后台强制确认',
  `confirmation_override_reason` varchar(255) DEFAULT NULL COMMENT '强制确认原因',
  `confirmation_override_by` bigint DEFAULT NULL COMMENT '强制确认管理员ID',
  `confirmation_override_time` datetime DEFAULT NULL COMMENT '强制确认时间',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_table_name` (`round_id`,`table_name`),
  KEY `idx_round_table_competition` (`competition_id`),
  KEY `idx_round_table_captain` (`captain_judge_id`),
  KEY `idx_round_table_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评审轮次桌表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `round_table_confirmation`
--

DROP TABLE IF EXISTS `round_table_confirmation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `round_table_confirmation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `round_table_id` bigint NOT NULL COMMENT '评审桌ID',
  `judge_account_id` bigint NOT NULL COMMENT '评委账号ID',
  `result_version` int NOT NULL COMMENT '结果版本号',
  `status` varchar(32) NOT NULL DEFAULT 'AGREED' COMMENT '确认状态',
  `confirmed_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '确认时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_table_confirmation_version` (`round_table_id`,`judge_account_id`,`result_version`),
  KEY `idx_round_table_confirmation_judge` (`judge_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评审桌结果确认表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `round_table_entry`
--

DROP TABLE IF EXISTS `round_table_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `round_table_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `round_id` bigint NOT NULL COMMENT '评审轮次ID',
  `round_table_id` bigint NOT NULL COMMENT '评审桌ID',
  `beer_entry_id` bigint NOT NULL COMMENT '参赛酒款ID',
  `source_round_table_id` bigint DEFAULT NULL COMMENT '来源评审桌ID',
  `status` varchar(32) NOT NULL COMMENT '桌内酒款状态',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_entry_once` (`round_id`,`beer_entry_id`),
  KEY `idx_round_table_entry_table` (`round_table_id`),
  KEY `idx_round_table_entry_competition` (`competition_id`),
  KEY `idx_round_table_entry_entry` (`beer_entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评审桌酒款分配表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `round_table_member`
--

DROP TABLE IF EXISTS `round_table_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `round_table_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `round_table_id` bigint NOT NULL COMMENT '评审桌ID',
  `judge_account_id` bigint NOT NULL COMMENT '评委账号ID',
  `role` varchar(32) NOT NULL COMMENT '角色',
  `system_task_required` tinyint NOT NULL DEFAULT '0' COMMENT '是否需要系统评分任务',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '轮次成员状态 ACTIVE/REMOVED',
  `removed_time` datetime DEFAULT NULL,
  `removed_by` bigint DEFAULT NULL,
  `remove_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_round_table_member` (`round_table_id`,`judge_account_id`),
  KEY `idx_round_table_member_judge` (`judge_account_id`),
  KEY `idx_round_table_member_status` (`round_table_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评审桌成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `score_record`
--

DROP TABLE IF EXISTS `score_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `score_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `round_id` bigint DEFAULT NULL,
  `round_table_id` bigint DEFAULT NULL,
  `beer_entry_id` bigint NOT NULL COMMENT '参赛酒款ID',
  `judge_account_id` bigint NOT NULL COMMENT '评委账号ID',
  `assignment_id` bigint DEFAULT NULL COMMENT '评委编排记录ID',
  `judge_role_type` varchar(32) NOT NULL COMMENT '评分时评委角色',
  `dimensions_json` json NOT NULL COMMENT '评分维度JSON',
  `total_score` decimal(6,2) NOT NULL DEFAULT '0.00' COMMENT '总分',
  `comments` varchar(1000) NOT NULL COMMENT '评分评语',
  `is_final` tinyint NOT NULL DEFAULT '0' COMMENT '是否最终评分',
  `is_advanced` tinyint NOT NULL DEFAULT '0' COMMENT '是否晋级',
  `consensus_score` decimal(6,2) DEFAULT NULL COMMENT '桌长共识分',
  `duration_seconds` int DEFAULT NULL,
  `comment_char_count` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_score_record_judge_entry_final` (`beer_entry_id`,`judge_account_id`,`is_final`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评委评分记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sms_code_log`
--

DROP TABLE IF EXISTS `sms_code_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sms_code_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone_hash` char(64) NOT NULL COMMENT '手机号哈希值',
  `masked_phone` varchar(20) NOT NULL COMMENT '脱敏手机号',
  `biz_type` varchar(32) NOT NULL COMMENT '短信业务类型',
  `status` varchar(16) NOT NULL COMMENT '发送状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sms_code_log_phone_hash` (`phone_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信验证码发送记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `style_category`
--

DROP TABLE IF EXISTS `style_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `style_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `library_id` bigint NOT NULL COMMENT '所属风格库ID',
  `name` varchar(128) NOT NULL COMMENT '风格分类名称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_style_category_name` (`library_id`,`name`),
  KEY `idx_style_category_library` (`library_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒风格分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `style_item`
--

DROP TABLE IF EXISTS `style_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `style_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `library_id` bigint NOT NULL COMMENT '所属风格库ID',
  `category_id` bigint DEFAULT NULL COMMENT '风格分类ID',
  `name` varchar(128) NOT NULL COMMENT '风格名称',
  `style_code` varchar(64) DEFAULT NULL COMMENT '风格编码',
  `description` text COMMENT '说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '风格启用状态',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_style_item_name` (`library_id`,`name`),
  KEY `idx_style_item_library` (`library_id`,`sort_order`),
  KEY `idx_style_item_category` (`category_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒风格条目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `style_library`
--

DROP TABLE IF EXISTS `style_library`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `style_library` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(64) NOT NULL COMMENT '风格库编码',
  `name` varchar(128) NOT NULL COMMENT '风格库名称',
  `version` varchar(64) NOT NULL COMMENT '风格库版本',
  `language` varchar(32) NOT NULL COMMENT '语言',
  `source` varchar(64) NOT NULL COMMENT '来源',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '风格库启用状态',
  `tags_json` json DEFAULT NULL COMMENT '标签JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `organizer_id` bigint NOT NULL COMMENT '所属主办方，平台库归属啤酒事务局',
  `visibility` varchar(16) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PUBLIC 公共库，PRIVATE 内部库',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_style_library_code` (`code`),
  KEY `idx_style_library_organizer` (`organizer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='啤酒风格库表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wechat_pay_notify`
--

DROP TABLE IF EXISTS `wechat_pay_notify`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wechat_pay_notify` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `notify_id` varchar(128) NOT NULL COMMENT '微信回调通知ID',
  `event_type` varchar(64) NOT NULL COMMENT '微信回调事件类型',
  `business_type` varchar(32) NOT NULL COMMENT '回调业务类型：PAYMENT/REFUND/BEER_COIN_PURCHASE',
  `out_trade_no` varchar(64) DEFAULT NULL COMMENT '商户支付单号',
  `out_refund_no` varchar(64) DEFAULT NULL COMMENT '商户退款单号',
  `wechat_transaction_id` varchar(64) DEFAULT NULL COMMENT '微信支付交易号',
  `wechat_refund_id` varchar(64) DEFAULT NULL COMMENT '微信退款单号',
  `raw_json` json NOT NULL COMMENT '原始报文JSON',
  `processed_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否已处理',
  `process_message` varchar(300) DEFAULT NULL COMMENT '处理结果说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wechat_pay_notify_id` (`notify_id`),
  KEY `idx_wechat_pay_notify_trade` (`out_trade_no`),
  KEY `idx_wechat_pay_notify_refund` (`out_refund_no`),
  KEY `idx_wechat_pay_notify_business` (`business_type`,`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='微信支付回调审计表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping events for database 'beer_competition'
--

--
-- Dumping routines for database 'beer_competition'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed
