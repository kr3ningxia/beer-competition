ALTER TABLE `award_result`
  ADD COLUMN `certificate_asset_id` bigint NULL DEFAULT NULL AFTER `published_time`,
  ADD COLUMN `certificate_uploaded_at` datetime NULL DEFAULT NULL AFTER `certificate_asset_id`,
  ADD COLUMN `certificate_filename` varchar(255) NULL DEFAULT NULL AFTER `certificate_uploaded_at`,
  ADD KEY `idx_award_result_certificate_asset` (`certificate_asset_id`);
