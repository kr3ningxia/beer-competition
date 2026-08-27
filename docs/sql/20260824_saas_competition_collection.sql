-- 租户赛事收款配置。执行前请确认 competition.organizer_id 已完成组织回填。
CREATE TABLE IF NOT EXISTS `competition_collection_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '租户赛事ID',
  `wechat_qr_asset_id` bigint DEFAULT NULL COMMENT '微信收款码文件资产ID',
  `bank_account_name` varchar(128) DEFAULT NULL COMMENT '收款账户名',
  `bank_account_no` varchar(128) DEFAULT NULL COMMENT '收款账号',
  `bank_name` varchar(128) DEFAULT NULL COMMENT '开户行',
  `collection_note` varchar(1000) DEFAULT NULL COMMENT '收款提示',
  `enabled_methods_json` varchar(255) NOT NULL COMMENT '启用收款方式 JSON 数组',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_competition` (`competition_id`),
  KEY `idx_collection_qr_asset` (`wechat_qr_asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户赛事收款配置';
