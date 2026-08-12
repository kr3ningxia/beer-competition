-- 修复比赛 1070 本批导入酒款的补充字段明细。可重复执行，不修改酒款 JSON 快照。
SET NAMES utf8mb4;
SET time_zone = '+08:00';
START TRANSACTION;

UPDATE beer_entry_extra_field x
JOIN beer_entry e ON e.id = x.beer_entry_id
JOIN entry_scan_label l ON l.beer_entry_id = e.id
  AND l.competition_id = 1070
  AND l.label_code LIKE 'BE-IMP1070-%'
JOIN entry_field_config c ON c.competition_id = e.competition_id
  AND c.field_key = x.field_key
  AND c.active_flag = 1
SET x.field_label = c.field_label,
    x.field_value = JSON_UNQUOTE(JSON_EXTRACT(e.extra_fields_json, CONCAT('$.', c.field_key)))
WHERE e.competition_id = 1070
  AND e.deleted_flag = 0
  AND JSON_VALID(e.extra_fields_json) = 1
  AND JSON_CONTAINS_PATH(e.extra_fields_json, 'one', CONCAT('$.', c.field_key)) = 1
  AND NULLIF(JSON_UNQUOTE(JSON_EXTRACT(e.extra_fields_json, CONCAT('$.', c.field_key))), '') IS NOT NULL;

INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT e.id,
       c.field_key,
       c.field_label,
       JSON_UNQUOTE(JSON_EXTRACT(e.extra_fields_json, CONCAT('$.', c.field_key)))
FROM beer_entry e
JOIN entry_scan_label l ON l.beer_entry_id = e.id
  AND l.competition_id = 1070
  AND l.label_code LIKE 'BE-IMP1070-%'
JOIN entry_field_config c ON c.competition_id = e.competition_id
  AND c.active_flag = 1
WHERE e.competition_id = 1070
  AND e.deleted_flag = 0
  AND JSON_VALID(e.extra_fields_json) = 1
  AND JSON_CONTAINS_PATH(e.extra_fields_json, 'one', CONCAT('$.', c.field_key)) = 1
  AND NULLIF(JSON_UNQUOTE(JSON_EXTRACT(e.extra_fields_json, CONCAT('$.', c.field_key))), '') IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM beer_entry_extra_field existing
      WHERE existing.beer_entry_id = e.id
        AND existing.field_key = c.field_key
  );

COMMIT;

SELECT x.field_key, COUNT(*) AS value_count, COUNT(DISTINCT x.beer_entry_id) AS entry_count
FROM beer_entry_extra_field x
JOIN beer_entry e ON e.id = x.beer_entry_id
JOIN entry_scan_label l ON l.beer_entry_id = e.id
WHERE e.competition_id = 1070
  AND e.deleted_flag = 0
  AND l.competition_id = 1070
  AND l.label_code LIKE 'BE-IMP1070-%'
GROUP BY x.field_key
ORDER BY x.field_key;
