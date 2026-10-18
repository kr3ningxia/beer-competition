ALTER TABLE competition
  ADD COLUMN brew_type varchar(16) NOT NULL DEFAULT 'COMMERCIAL' COMMENT '酿造类型：COMMERCIAL 商酿 / HOMEBREW 家酿 / MIXED 商酿家酿混合' AFTER competition_type;
