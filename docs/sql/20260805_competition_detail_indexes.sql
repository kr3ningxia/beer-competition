-- Competition detail progress and round workspace indexes.

ALTER TABLE `score_record`
  ADD KEY `idx_score_record_comp_round_table_final` (`competition_id`, `round_id`, `round_table_id`, `is_final`),
  ADD KEY `idx_score_record_comp_entry_final` (`competition_id`, `beer_entry_id`, `is_final`, `id`);

ALTER TABLE `round_result`
  ADD KEY `idx_round_result_round_table` (`round_id`, `round_table_id`);
