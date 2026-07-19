-- Competition management list aggregation index.
ALTER TABLE beer_entry
  ADD KEY idx_beer_entry_competition_status_stored (competition_id, status, stored_flag);
