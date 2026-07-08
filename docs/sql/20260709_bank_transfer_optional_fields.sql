-- 银行转账付款信息简化：厂商端不再要求填写付款时间，转账备注改为选填。

ALTER TABLE bank_transfer_payment
  MODIFY COLUMN transfer_time datetime DEFAULT NULL COMMENT '银行转账时间',
  MODIFY COLUMN remark varchar(255) DEFAULT NULL COMMENT '备注';
