-- 租户赛事报名收款审核补充字段
-- payment_order 当前已有 bank_transfer_id，可复用关联待确认记录。
ALTER TABLE payment_order
    ADD COLUMN manual_submit_time DATETIME NULL COMMENT '主办方收款码付款申报时间',
    ADD COLUMN manual_confirmed_by_admin_id BIGINT NULL COMMENT '主办方收款确认人',
    ADD COLUMN manual_confirmed_time DATETIME NULL COMMENT '主办方收款确认时间',
    ADD COLUMN manual_confirm_remark VARCHAR(255) NULL COMMENT '主办方收款审核备注';

ALTER TABLE bank_transfer_payment
    ADD COLUMN collection_snapshot_json JSON NULL COMMENT '提交时收款配置快照';
