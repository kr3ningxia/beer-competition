-- 后台酒款列表：关联筛选与最新退款、标签、分桌判断所需索引。

ALTER TABLE `entry_refund`
  ADD KEY `idx_entry_refund_entry_latest` (`beer_entry_id`, `id`);

ALTER TABLE `round_table_entry`
  ADD KEY `idx_round_table_entry_entry` (`beer_entry_id`);
