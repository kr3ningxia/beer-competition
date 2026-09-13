ALTER TABLE `competition_round`
  ADD COLUMN `allocation_revision` BIGINT NOT NULL DEFAULT 0 COMMENT '轮次编排修订号' AFTER `update_time`;
