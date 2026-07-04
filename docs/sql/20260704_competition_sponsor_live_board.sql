CREATE TABLE IF NOT EXISTS competition_sponsor (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  competition_id bigint NOT NULL COMMENT '所属比赛ID',
  tier_label varchar(32) NOT NULL COMMENT '赞助等级',
  sponsor_name varchar(64) NOT NULL COMMENT '赞助商名称',
  logo_asset_id bigint DEFAULT NULL COMMENT 'Logo 文件资产ID',
  sort_order int NOT NULL DEFAULT 0 COMMENT '展示排序',
  featured_flag tinyint NOT NULL DEFAULT 0 COMMENT '是否重点展示',
  enabled_flag tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_competition_sponsor_competition (competition_id, enabled_flag, sort_order),
  KEY idx_competition_sponsor_logo (logo_asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛赞助商配置表';
