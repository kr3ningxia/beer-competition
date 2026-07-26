-- 报名中配置调整与管理端酒款删除支持

ALTER TABLE `competition_style_config`
  DROP INDEX `uk_competition_style_name`,
  ADD COLUMN `active_flag` tinyint NOT NULL DEFAULT 1 COMMENT '是否为当前可报名风格' AFTER `sort_order`,
  ADD COLUMN `source_library_version` varchar(64) DEFAULT NULL COMMENT '来源风格库版本' AFTER `active_flag`,
  ADD KEY `idx_competition_style_active` (`competition_id`, `active_flag`, `sort_order`),
  ADD KEY `idx_competition_style_name` (`competition_id`, `name`);

UPDATE `competition_style_config` c
JOIN `competition` p ON p.`id` = c.`competition_id`
SET c.`source_library_version` = p.`style_library_version`
WHERE c.`source_library_version` IS NULL;

ALTER TABLE `entry_field_config`
  ADD COLUMN `active_flag` tinyint NOT NULL DEFAULT 1 COMMENT '是否为当前报名字段' AFTER `sort_order`,
  ADD KEY `idx_entry_field_active` (`competition_id`, `active_flag`, `sort_order`);

ALTER TABLE `beer_entry`
  ADD COLUMN `style_config_id` bigint DEFAULT NULL COMMENT '报名时选择的比赛风格快照ID' AFTER `style`,
  ADD COLUMN `deleted_flag` tinyint NOT NULL DEFAULT 0 COMMENT '管理端业务删除标记' AFTER `stored_flag`,
  ADD COLUMN `deleted_time` datetime DEFAULT NULL COMMENT '管理端删除时间' AFTER `deleted_flag`,
  ADD COLUMN `deleted_by_admin_id` bigint DEFAULT NULL COMMENT '执行删除的管理员ID' AFTER `deleted_time`,
  ADD COLUMN `delete_reason` varchar(500) DEFAULT NULL COMMENT '管理端删除原因' AFTER `deleted_by_admin_id`,
  ADD KEY `idx_beer_entry_active_competition` (`deleted_flag`, `competition_id`, `status`),
  ADD KEY `idx_beer_entry_active_brewery` (`deleted_flag`, `brewery_id`, `id`);

UPDATE `beer_entry` e
JOIN `competition_style_config` s
  ON s.`competition_id` = e.`competition_id`
 AND s.`name` = e.`style`
SET e.`style_config_id` = s.`id`
WHERE e.`style_config_id` IS NULL;
