ALTER TABLE judge_account
  ADD COLUMN brewery_conflict_flag tinyint NOT NULL DEFAULT 0 COMMENT '是否存在酒厂利益关系' AFTER qualification,
  ADD COLUMN brewery_conflict_text varchar(500) NULL COMMENT '相关酒厂或品牌及关系说明' AFTER brewery_conflict_flag;
