SET NAMES utf8mb4;

SELECT 'competition_entries' check_item, COUNT(*) value
FROM beer_entry
WHERE competition_id = 1070 AND deleted_flag = 0;

SELECT 'competition_labels' check_item, COUNT(*) value
FROM entry_scan_label
WHERE competition_id = 1070 AND status = 'ACTIVE';

SELECT 'target_entries' check_item, COUNT(DISTINCT entry.id) value
FROM beer_entry entry
JOIN entry_scan_label label ON label.beer_entry_id = entry.id
WHERE entry.competition_id = 1070
  AND entry.deleted_flag = 0
  AND label.competition_id = 1070
  AND label.short_code IN ('85079','55986','76015');

SELECT 'short_code_duplicates' check_item, COUNT(*) value
FROM (
  SELECT short_code
  FROM entry_scan_label
  WHERE competition_id = 1070
  GROUP BY short_code
  HAVING COUNT(*) > 1
) duplicated;

SELECT 'scan_token_duplicates' check_item, COUNT(*) value
FROM (
  SELECT scan_token
  FROM entry_scan_label
  WHERE competition_id = 1070
  GROUP BY scan_token
  HAVING COUNT(*) > 1
) duplicated;

SELECT 'invalid_target_labels' check_item, COUNT(*) value
FROM entry_scan_label
WHERE competition_id = 1070
  AND short_code IN ('85079','55986','76015')
  AND (status <> 'ACTIVE' OR CHAR_LENGTH(short_code) <> 5
       OR short_code NOT REGEXP '^[0-9]{5}$'
       OR scan_token IS NULL OR scan_token = '');

SELECT 'target_extra_field_missing' check_item, COUNT(*) value
FROM (
  SELECT entry.id
  FROM beer_entry entry
  JOIN entry_scan_label label ON label.beer_entry_id = entry.id
  LEFT JOIN beer_entry_extra_field extra ON extra.beer_entry_id = entry.id
  WHERE entry.competition_id = 1070
    AND entry.deleted_flag = 0
    AND label.competition_id = 1070
    AND label.short_code IN ('85079','55986','76015')
  GROUP BY entry.id
  HAVING COUNT(DISTINCT extra.field_key) <> 2
) missing;

SELECT 'target_style_link_missing' check_item, COUNT(*) value
FROM beer_entry entry
JOIN entry_scan_label label ON label.beer_entry_id = entry.id
LEFT JOIN competition_style_config style
  ON style.id = entry.style_config_id
 AND style.competition_id = entry.competition_id
WHERE entry.competition_id = 1070
  AND entry.deleted_flag = 0
  AND label.short_code IN ('85079','55986','76015')
  AND (style.id IS NULL OR style.active_flag <> 1 OR style.name <> entry.style);

SELECT 'target_account_missing' check_item, COUNT(*) value
FROM beer_entry entry
JOIN entry_scan_label label ON label.beer_entry_id = entry.id
LEFT JOIN portal_account account ON account.brewery_id = entry.brewery_id AND account.status = 1
WHERE entry.competition_id = 1070
  AND entry.deleted_flag = 0
  AND label.short_code IN ('85079','55986','76015')
  AND account.id IS NULL;

SELECT label.short_code,
       entry.name,
       brewery.company_name,
       brewery.contact_name,
       CONCAT('***', RIGHT(account.phone, 4)) account_phone,
       entry.style,
       entry.abv,
       JSON_UNQUOTE(JSON_EXTRACT(entry.extra_fields_json, '$.specialIngredients')) special_ingredients,
       JSON_UNQUOTE(JSON_EXTRACT(entry.extra_fields_json, '$.custom_1785549677921_0089y')) promotion,
       label.status
FROM beer_entry entry
JOIN brewery brewery ON brewery.id = entry.brewery_id
JOIN entry_scan_label label ON label.beer_entry_id = entry.id
LEFT JOIN portal_account account ON account.brewery_id = brewery.id
WHERE entry.competition_id = 1070
  AND entry.deleted_flag = 0
  AND label.short_code IN ('85079','55986','76015')
ORDER BY label.short_code, account.id;

SELECT field_key, required_flag, visible_to_judges, active_flag
FROM entry_field_config
WHERE competition_id = 1070
  AND field_key IN ('specialIngredients','custom_1785549677921_0089y')
ORDER BY sort_order;
