-- 租户赛事收款配置：新增「付款备注必填」开关。
-- 开启后，厂商提交收款码付款确认或银行转账信息时必须填写付款备注。
ALTER TABLE `competition_collection_config`
  ADD COLUMN `payment_remark_required` tinyint NOT NULL DEFAULT 0 COMMENT '是否要求厂商填写付款备注：1=必填' AFTER `skip_payment_confirm`;
