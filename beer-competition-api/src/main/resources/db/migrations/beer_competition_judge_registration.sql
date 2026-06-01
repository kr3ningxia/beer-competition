ALTER TABLE `judge_account`
  ADD COLUMN `submitted_time` datetime DEFAULT NULL AFTER `status`,
  ADD COLUMN `reviewed_time` datetime DEFAULT NULL AFTER `submitted_time`,
  ADD COLUMN `reviewed_by` bigint DEFAULT NULL AFTER `reviewed_time`,
  ADD COLUMN `review_remark` varchar(255) DEFAULT NULL AFTER `reviewed_by`;
