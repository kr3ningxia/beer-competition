-- 风格库公共/内部可见范围。
ALTER TABLE style_library
  ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PUBLIC 公共库，PRIVATE 内部库' AFTER organizer_id;

UPDATE style_library
SET organizer_id = 1
WHERE organizer_id IS NULL;

UPDATE style_library
SET visibility = CASE
  WHEN code = 'CUSTOM_STANDARD' THEN 'PRIVATE'
  ELSE 'PUBLIC'
END
WHERE code IN ('BJCP_2021_CN', 'BJCP_2021_EN', 'BBC_2026_CN', 'BJCP_2021_FULL_CN', 'BJCP_BA_2026_CN');
