ALTER TABLE competition
    ADD COLUMN refund_approval_mode varchar(32) NOT NULL DEFAULT 'AUTO_APPROVE'
        COMMENT '退款申请审批方式：AUTO_APPROVE、MANUAL_REVIEW'
        AFTER early_bird_deadline;

ALTER TABLE entry_refund
    ADD COLUMN approval_mode_snapshot varchar(32) DEFAULT NULL
        COMMENT '申请退款时比赛采用的审批方式'
        AFTER status;
