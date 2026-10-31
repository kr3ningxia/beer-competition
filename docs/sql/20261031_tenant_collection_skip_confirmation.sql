-- 租户赛事收款配置：新增「跳过付款确认」开关。
-- 开启后，厂商在收款码付款页点击「我已完成付款」即视为支付成功，无需主办方后台二次确认。
ALTER TABLE `competition_collection_config`
  ADD COLUMN `skip_payment_confirm` tinyint NOT NULL DEFAULT 0 COMMENT '是否跳过付款确认：1=厂商确认付款后自动到账' AFTER `payment_contact`;
