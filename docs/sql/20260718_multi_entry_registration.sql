-- 多酒款批次报名与聚合支付。
-- 采用增量结构并回填历史单款订单，保留原字段用于旧接口兼容。

CREATE TABLE IF NOT EXISTS registration_batch (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  batch_no varchar(64) NOT NULL COMMENT '报名批次号',
  competition_id bigint NOT NULL COMMENT '所属比赛ID',
  brewery_id bigint NOT NULL COMMENT '厂牌ID',
  portal_account_id bigint NOT NULL COMMENT '提交账号ID',
  entry_count int NOT NULL COMMENT '酒款数量',
  total_amount decimal(10,2) NOT NULL COMMENT '批次应付总额',
  status varchar(32) NOT NULL COMMENT '批次状态',
  idempotency_key varchar(64) NOT NULL COMMENT '客户端幂等键',
  rules_accepted tinyint NOT NULL DEFAULT 0 COMMENT '是否同意参赛细则',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_registration_batch_no (batch_no),
  UNIQUE KEY uk_registration_batch_idempotency (portal_account_id, idempotency_key),
  KEY idx_registration_batch_brewery (brewery_id, create_time),
  KEY idx_registration_batch_competition (competition_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多酒款报名批次表';

CREATE TABLE IF NOT EXISTS payment_order (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  order_no varchar(64) NOT NULL COMMENT '系统支付订单号',
  registration_batch_id bigint NOT NULL COMMENT '报名批次ID',
  amount decimal(10,2) NOT NULL COMMENT '订单应付总额',
  paid_amount decimal(10,2) DEFAULT NULL COMMENT '实付金额',
  refunded_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计退款金额',
  status varchar(32) NOT NULL COMMENT '订单状态',
  pay_method varchar(32) NOT NULL COMMENT '支付方式',
  out_trade_no varchar(64) DEFAULT NULL COMMENT '微信商户支付单号',
  wechat_transaction_id varchar(64) DEFAULT NULL COMMENT '微信支付交易号',
  bank_transfer_id bigint DEFAULT NULL COMMENT '银行转账付款单ID',
  code_url varchar(512) DEFAULT NULL COMMENT '微信Native支付二维码链接',
  expire_time datetime DEFAULT NULL COMMENT '订单过期时间',
  wechat_trade_state varchar(32) DEFAULT NULL COMMENT '微信交易状态',
  wechat_trade_state_desc varchar(128) DEFAULT NULL COMMENT '微信交易状态描述',
  notify_raw_json json DEFAULT NULL COMMENT '支付通知原始JSON',
  last_query_time datetime DEFAULT NULL COMMENT '最近查询时间',
  paid_time datetime DEFAULT NULL COMMENT '支付完成时间',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_order_no (order_no),
  UNIQUE KEY uk_payment_order_batch (registration_batch_id),
  UNIQUE KEY uk_payment_order_out_trade_no (out_trade_no),
  KEY idx_payment_order_status (status, update_time),
  KEY idx_payment_order_bank_transfer (bank_transfer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聚合报名支付订单表';

CREATE TABLE IF NOT EXISTS payment_order_item (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  payment_order_id bigint NOT NULL COMMENT '聚合支付订单ID',
  beer_entry_id bigint NOT NULL COMMENT '参赛酒款ID',
  entry_payment_id bigint NOT NULL COMMENT '单款应收记录ID',
  amount decimal(10,2) NOT NULL COMMENT '酒款分摊金额',
  refunded_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '酒款累计退款金额',
  status varchar(32) NOT NULL COMMENT '分项状态',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_order_item_entry (beer_entry_id),
  UNIQUE KEY uk_payment_order_item_payment (entry_payment_id),
  KEY idx_payment_order_item_order (payment_order_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聚合支付订单酒款明细表';

ALTER TABLE beer_entry
  ADD COLUMN registration_batch_id bigint NULL COMMENT '报名批次ID' AFTER brewery_id,
  ADD KEY idx_beer_entry_batch (registration_batch_id);

ALTER TABLE entry_payment
  ADD COLUMN payment_order_id bigint NULL COMMENT '聚合支付订单ID' AFTER beer_entry_id,
  ADD KEY idx_entry_payment_order (payment_order_id);

ALTER TABLE bank_transfer_payment
  MODIFY COLUMN beer_entry_id bigint NULL,
  MODIFY COLUMN entry_payment_id bigint NULL,
  ADD COLUMN payment_order_id bigint NULL COMMENT '聚合支付订单ID' AFTER competition_id,
  ADD KEY idx_bank_transfer_payment_order (payment_order_id);

ALTER TABLE entry_refund
  ADD COLUMN payment_order_item_id bigint NULL COMMENT '聚合订单酒款明细ID' AFTER entry_payment_id,
  ADD KEY idx_entry_refund_order_item (payment_order_item_id);

-- 修复历史酒款缺失的单款应收记录。
INSERT INTO entry_payment (beer_entry_id, amount, status, pay_method, paid_amount, paid_time, confirm_remark)
SELECT e.id,
       c.entry_fee,
       CASE
         WHEN e.status IN ('REGISTERED', 'STORED', 'RESULT_PUBLISHED') THEN 'PAID'
         WHEN e.status = 'CANCELED' THEN 'CANCELED'
         ELSE 'UNPAID'
       END,
       'MANUAL',
       CASE WHEN e.status IN ('REGISTERED', 'STORED', 'RESULT_PUBLISHED') THEN c.entry_fee ELSE NULL END,
       CASE WHEN e.status IN ('REGISTERED', 'STORED', 'RESULT_PUBLISHED') THEN e.update_time ELSE NULL END,
       '历史数据兼容补建'
FROM beer_entry e
JOIN competition c ON c.id = e.competition_id
LEFT JOIN entry_payment ep ON ep.beer_entry_id = e.id
WHERE ep.id IS NULL;

-- 历史记录按“一款酒一个批次”回填，确保统一查询和退款逻辑可用。
INSERT IGNORE INTO registration_batch
  (batch_no, competition_id, brewery_id, portal_account_id, entry_count, total_amount,
   status, idempotency_key, rules_accepted, create_time, update_time)
SELECT CONCAT('LEGACY-B-', e.id),
       e.competition_id,
       e.brewery_id,
       COALESCE((SELECT MIN(pa.id) FROM portal_account pa WHERE pa.brewery_id = e.brewery_id), 0),
       1,
       ep.amount,
       CASE ep.status
         WHEN 'PAID' THEN 'PAID'
         WHEN 'REFUNDED' THEN 'REFUNDED'
         WHEN 'CANCELED' THEN 'CANCELED'
         ELSE 'PENDING_PAYMENT'
       END,
       CONCAT('LEGACY-', e.id),
       1,
       e.create_time,
       e.update_time
FROM beer_entry e
JOIN entry_payment ep ON ep.beer_entry_id = e.id;

UPDATE beer_entry e
JOIN registration_batch rb ON rb.batch_no = CONCAT('LEGACY-B-', e.id)
SET e.registration_batch_id = rb.id
WHERE e.registration_batch_id IS NULL;

INSERT IGNORE INTO payment_order
  (order_no, registration_batch_id, amount, paid_amount, refunded_amount, status, pay_method,
   out_trade_no, wechat_transaction_id, bank_transfer_id, code_url, expire_time,
   wechat_trade_state, wechat_trade_state_desc, notify_raw_json, last_query_time, paid_time,
   create_time, update_time)
SELECT CONCAT('LEGACY-O-', ep.id),
       rb.id,
       ep.amount,
       ep.paid_amount,
       CASE WHEN ep.status = 'REFUNDED' THEN ep.amount ELSE 0.00 END,
       CASE ep.status
         WHEN 'PAID' THEN 'PAID'
         WHEN 'REFUNDED' THEN 'REFUNDED'
         WHEN 'PENDING_CONFIRM' THEN 'PENDING_CONFIRM'
         WHEN 'CANCELED' THEN 'CANCELED'
         ELSE 'UNPAID'
       END,
       ep.pay_method,
       ep.out_trade_no,
       ep.wechat_transaction_id,
       ep.bank_transfer_id,
       ep.code_url,
       ep.expire_time,
       ep.wechat_trade_state,
       ep.wechat_trade_state_desc,
       ep.notify_raw_json,
       ep.last_query_time,
       ep.paid_time,
       ep.create_time,
       ep.update_time
FROM entry_payment ep
JOIN beer_entry e ON e.id = ep.beer_entry_id
JOIN registration_batch rb ON rb.id = e.registration_batch_id;

UPDATE entry_payment ep
JOIN payment_order po ON po.order_no = CONCAT('LEGACY-O-', ep.id)
SET ep.payment_order_id = po.id
WHERE ep.payment_order_id IS NULL;

INSERT IGNORE INTO payment_order_item
  (payment_order_id, beer_entry_id, entry_payment_id, amount, refunded_amount, status, create_time, update_time)
SELECT po.id,
       ep.beer_entry_id,
       ep.id,
       ep.amount,
       CASE WHEN ep.status = 'REFUNDED' THEN ep.amount ELSE 0.00 END,
       ep.status,
       ep.create_time,
       ep.update_time
FROM entry_payment ep
JOIN payment_order po ON po.id = ep.payment_order_id;

UPDATE entry_refund er
JOIN payment_order_item poi ON poi.entry_payment_id = er.entry_payment_id
SET er.payment_order_item_id = poi.id
WHERE er.payment_order_item_id IS NULL;

UPDATE bank_transfer_payment btp
JOIN entry_payment ep ON ep.id = btp.entry_payment_id
SET btp.payment_order_id = ep.payment_order_id
WHERE btp.payment_order_id IS NULL;
