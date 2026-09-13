-- 评委账号增加选填的 BJCP 编号。

ALTER TABLE `judge_account`
  ADD COLUMN `bjcp_number` varchar(64) DEFAULT NULL COMMENT 'BJCP编号' AFTER `qualification`;
