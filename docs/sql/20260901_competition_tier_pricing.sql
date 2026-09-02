-- 比赛累计阶梯报名价及退款计价快照
ALTER TABLE competition
  ADD COLUMN tier_pricing_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用累计阶梯报名价' AFTER early_bird_deadline;

CREATE TABLE IF NOT EXISTS competition_fee_tier (
  id BIGINT NOT NULL AUTO_INCREMENT,
  competition_id BIGINT NOT NULL,
  start_quantity INT NOT NULL COMMENT '从第几款起生效',
  discount_rate DECIMAL(8,4) NOT NULL COMMENT '折扣率，1.0000 为原价',
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_competition_fee_tier_start (competition_id, start_quantity),
  KEY idx_competition_fee_tier_competition (competition_id, enabled, start_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛累计阶梯报名价';

ALTER TABLE entry_payment
  ADD COLUMN pricing_base_amount DECIMAL(10,2) NULL COMMENT '计价基础单价快照',
  ADD COLUMN discount_rate DECIMAL(8,4) NULL COMMENT '成交折扣率快照',
  ADD COLUMN pricing_sequence INT NULL COMMENT '本场本厂牌累计序号';

ALTER TABLE payment_order_item
  ADD COLUMN pricing_base_amount DECIMAL(10,2) NULL COMMENT '计价基础单价快照',
  ADD COLUMN discount_rate DECIMAL(8,4) NULL COMMENT '成交折扣率快照',
  ADD COLUMN pricing_sequence INT NULL COMMENT '本场本厂牌累计序号';
