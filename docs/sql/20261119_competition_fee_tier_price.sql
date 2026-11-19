-- 阶梯报名价改为按“折后单价（元/款）”维护，折扣率作为派生值保留，供早鸟价折上折计价。
ALTER TABLE `competition_fee_tier`
  ADD COLUMN `tier_price` DECIMAL(10,2) NULL
    COMMENT '折后单价（元/款），阶梯价以金额为准'
    AFTER `start_quantity`;

-- 历史数据按当前报名费把折扣率换算成金额；若报名费此后调整过，换算结果以当前报名费为准。
UPDATE `competition_fee_tier` t
JOIN `competition` c ON c.id = t.competition_id
SET t.tier_price = ROUND(c.entry_fee * t.discount_rate, 2)
WHERE t.tier_price IS NULL;
