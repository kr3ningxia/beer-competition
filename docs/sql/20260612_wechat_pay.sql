ALTER TABLE entry_payment
  ADD COLUMN code_url varchar(512) DEFAULT NULL AFTER wechat_transaction_id,
  ADD COLUMN expire_time datetime DEFAULT NULL AFTER code_url,
  ADD COLUMN paid_amount decimal(10,2) DEFAULT NULL AFTER expire_time,
  ADD COLUMN wechat_trade_state varchar(32) DEFAULT NULL AFTER paid_amount,
  ADD COLUMN wechat_trade_state_desc varchar(128) DEFAULT NULL AFTER wechat_trade_state,
  ADD COLUMN notify_raw_json json DEFAULT NULL AFTER wechat_trade_state_desc,
  ADD COLUMN last_query_time datetime DEFAULT NULL AFTER notify_raw_json,
  ADD UNIQUE KEY uk_entry_payment_out_trade_no (out_trade_no),
  ADD KEY idx_entry_payment_status (status, update_time);

ALTER TABLE entry_refund
  ADD COLUMN wechat_refund_status varchar(32) DEFAULT NULL AFTER wechat_refund_id,
  ADD COLUMN refund_notify_raw_json json DEFAULT NULL AFTER notify_raw_json,
  ADD COLUMN last_query_time datetime DEFAULT NULL AFTER refund_notify_raw_json,
  ADD UNIQUE KEY uk_entry_refund_out_refund_no (out_refund_no);

CREATE TABLE IF NOT EXISTS wechat_pay_notify (
  id bigint NOT NULL AUTO_INCREMENT,
  notify_id varchar(128) NOT NULL,
  event_type varchar(64) NOT NULL,
  business_type varchar(32) NOT NULL,
  out_trade_no varchar(64) DEFAULT NULL,
  out_refund_no varchar(64) DEFAULT NULL,
  wechat_transaction_id varchar(64) DEFAULT NULL,
  wechat_refund_id varchar(64) DEFAULT NULL,
  raw_json json NOT NULL,
  processed_flag tinyint NOT NULL DEFAULT 0,
  process_message varchar(300) DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wechat_pay_notify_id (notify_id),
  KEY idx_wechat_pay_notify_trade (out_trade_no),
  KEY idx_wechat_pay_notify_refund (out_refund_no),
  KEY idx_wechat_pay_notify_business (business_type, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='微信支付回调审计与幂等记录';
