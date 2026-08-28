-- 组织私有风格库：每个风格库归属一个主办方组织。
ALTER TABLE style_library
  ADD COLUMN organizer_id BIGINT NULL COMMENT '所属主办方，迁移后平台公共库归属啤酒事务局',
  ADD KEY idx_style_library_organizer (organizer_id);

UPDATE style_library
SET organizer_id = 1
WHERE organizer_id IS NULL;

ALTER TABLE style_library
  MODIFY COLUMN organizer_id BIGINT NOT NULL COMMENT '所属主办方，平台库归属啤酒事务局';
