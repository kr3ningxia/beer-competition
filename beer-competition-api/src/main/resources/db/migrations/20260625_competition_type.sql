ALTER TABLE competition
    ADD COLUMN competition_type varchar(32) NOT NULL DEFAULT 'AWARD' COMMENT '比赛类型' AFTER status;
