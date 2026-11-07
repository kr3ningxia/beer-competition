-- 折扣率精度由 4 位提升到 6 位。
-- 6 位可保证三位数报名费按任意两位小数金额反算折扣率后精确还原到分（最多支持到 9999.99 元）。
ALTER TABLE competition_fee_tier
  MODIFY COLUMN discount_rate DECIMAL(8,6) NOT NULL COMMENT '折扣率，1.000000 为原价';

ALTER TABLE entry_payment
  MODIFY COLUMN discount_rate DECIMAL(8,6) NULL COMMENT '成交折扣率快照';

ALTER TABLE payment_order_item
  MODIFY COLUMN discount_rate DECIMAL(8,6) NULL COMMENT '成交折扣率快照';
