DELETE sr
FROM `score_record` sr
JOIN (
  SELECT `beer_entry_id`, `judge_account_id`, `is_final`, MIN(`id`) AS keep_id
  FROM `score_record`
  GROUP BY `beer_entry_id`, `judge_account_id`, `is_final`
  HAVING COUNT(*) > 1
) dup
  ON dup.`beer_entry_id` = sr.`beer_entry_id`
 AND dup.`judge_account_id` = sr.`judge_account_id`
 AND dup.`is_final` = sr.`is_final`
WHERE sr.`id` <> dup.`keep_id`;

ALTER TABLE `score_record`
  ADD UNIQUE INDEX `uk_score_record_judge_entry_final`(`beer_entry_id`, `judge_account_id`, `is_final`) USING BTREE;
