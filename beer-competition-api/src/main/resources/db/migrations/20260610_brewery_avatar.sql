ALTER TABLE `brewery`
  ADD COLUMN `avatar_asset_id` bigint NULL DEFAULT NULL AFTER `wechat`,
  ADD COLUMN `avatar_url` varchar(500) NULL DEFAULT NULL AFTER `avatar_asset_id`;
