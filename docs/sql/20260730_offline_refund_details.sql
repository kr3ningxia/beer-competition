ALTER TABLE `entry_refund`
  ADD COLUMN `offline_refund_account_name` varchar(128) DEFAULT NULL COMMENT '线下退款收款人' AFTER `approval_mode_snapshot`,
  ADD COLUMN `offline_refund_bank_name` varchar(128) DEFAULT NULL COMMENT '线下退款收款银行' AFTER `offline_refund_account_name`,
  ADD COLUMN `offline_refund_account_no_enc` varchar(512) DEFAULT NULL COMMENT '线下退款收款账号密文' AFTER `offline_refund_bank_name`,
  ADD COLUMN `offline_refund_account_no_last4` varchar(8) DEFAULT NULL COMMENT '线下退款收款账号后四位' AFTER `offline_refund_account_no_enc`,
  ADD COLUMN `offline_refund_transfer_no` varchar(128) DEFAULT NULL COMMENT '线下退款流水号' AFTER `offline_refund_account_no_last4`,
  ADD COLUMN `offline_refund_time` datetime DEFAULT NULL COMMENT '实际线下打款时间' AFTER `offline_refund_transfer_no`,
  ADD COLUMN `offline_refund_voucher_asset_id` bigint DEFAULT NULL COMMENT '线下退款打款凭证文件ID' AFTER `offline_refund_time`;
