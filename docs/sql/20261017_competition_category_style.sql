CREATE TABLE IF NOT EXISTS `competition_category_style` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `competition_id` bigint NOT NULL COMMENT '所属比赛ID',
  `category_id` bigint NOT NULL COMMENT '投递组别ID',
  `style_name` varchar(128) NOT NULL COMMENT '可选基础风格名称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_competition_category_style` (`category_id`,`style_name`),
  KEY `idx_competition_category_style` (`competition_id`,`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='比赛投递组别可选风格范围';
