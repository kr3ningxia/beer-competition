-- 租户赛事收款配置：新增「付款咨询联系」，供厂商付款遇到问题时联系主办方。
-- 仅租户赛事使用；平台自办赛事沿用 BankTransferAccountVO 中的固定小秘书微信。
ALTER TABLE `competition_collection_config`
  ADD COLUMN `payment_contact` varchar(128) DEFAULT NULL COMMENT '付款咨询联系（微信/电话）' AFTER `collection_note`;
