-- Competition 1070: import the three entries submitted on 2026-08-03.
SET NAMES utf8mb4;
SET time_zone = '+08:00';
START TRANSACTION;

-- 1. Reuse the existing brewery/account for Cangshen; create the other two.
SET @brewery_cangshen = (
  SELECT MIN(b.id)
  FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id = b.id
  WHERE a.phone = '18622027553' OR b.phone = '18622027553'
     OR b.company_name = CONVERT(0xe88b8de7a59ee985bfe980a0 USING utf8mb4)
);
UPDATE brewery
SET company_name = CONVERT(0xe88b8de7a59ee985bfe980a0 USING utf8mb4),
    contact_name = CONVERT(0xe9bd90e69687e58589 USING utf8mb4),
    phone = '18622027553',
    wechat = CONVERT(0x73746576656e37353533 USING utf8mb4)
WHERE id = @brewery_cangshen;
UPDATE portal_account
SET brewery_id = @brewery_cangshen,
    display_name = CONVERT(0xe88b8de7a59ee985bfe980a0 USING utf8mb4),
    wechat = CONVERT(0x73746576656e37353533 USING utf8mb4),
    status = 1
WHERE phone = '18622027553';

SET @brewery_no_why = (
  SELECT MIN(b.id)
  FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id = b.id
  WHERE a.phone = '15622203218' OR b.phone = '15622203218'
     OR b.company_name = CONVERT(0x4e4f20574859 USING utf8mb4)
);
INSERT INTO brewery (company_name, contact_name, phone, wechat)
SELECT CONVERT(0x4e4f20574859 USING utf8mb4),
       CONVERT(0x616c6c656e USING utf8mb4),
       '15622203218',
       NULL
WHERE @brewery_no_why IS NULL;
SET @brewery_no_why = (
  SELECT MIN(b.id)
  FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id = b.id
  WHERE a.phone = '15622203218' OR b.phone = '15622203218'
     OR b.company_name = CONVERT(0x4e4f20574859 USING utf8mb4)
);
UPDATE brewery
SET company_name = CONVERT(0x4e4f20574859 USING utf8mb4),
    contact_name = CONVERT(0x616c6c656e USING utf8mb4),
    phone = '15622203218',
    wechat = NULL
WHERE id = @brewery_no_why;
INSERT INTO portal_account (phone, wechat, display_name, brewery_id, status)
SELECT '15622203218', NULL, CONVERT(0x4e4f20574859 USING utf8mb4), @brewery_no_why, 1
WHERE NOT EXISTS (SELECT 1 FROM portal_account WHERE phone = '15622203218');
UPDATE portal_account
SET brewery_id = @brewery_no_why,
    display_name = CONVERT(0x4e4f20574859 USING utf8mb4),
    wechat = NULL,
    status = 1
WHERE phone = '15622203218';

SET @brewery_miaoxiu = (
  SELECT MIN(b.id)
  FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id = b.id
  WHERE a.phone = '13351186381' OR b.phone = '13351186381'
     OR b.company_name = CONVERT(0xe79c87e4bfae USING utf8mb4)
);
INSERT INTO brewery (company_name, contact_name, phone, wechat)
SELECT CONVERT(0xe79c87e4bfae USING utf8mb4),
       CONVERT(0xe6a88ae58588e7949f USING utf8mb4),
       '13351186381',
       CONVERT(0x6666663133333531313836333831 USING utf8mb4)
WHERE @brewery_miaoxiu IS NULL;
SET @brewery_miaoxiu = (
  SELECT MIN(b.id)
  FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id = b.id
  WHERE a.phone = '13351186381' OR b.phone = '13351186381'
     OR b.company_name = CONVERT(0xe79c87e4bfae USING utf8mb4)
);
UPDATE brewery
SET company_name = CONVERT(0xe79c87e4bfae USING utf8mb4),
    contact_name = CONVERT(0xe6a88ae58588e7949f USING utf8mb4),
    phone = '13351186381',
    wechat = CONVERT(0x6666663133333531313836333831 USING utf8mb4)
WHERE id = @brewery_miaoxiu;
INSERT INTO portal_account (phone, wechat, display_name, brewery_id, status)
SELECT '13351186381',
       CONVERT(0x6666663133333531313836333831 USING utf8mb4),
       CONVERT(0xe79c87e4bfae USING utf8mb4),
       @brewery_miaoxiu,
       1
WHERE NOT EXISTS (SELECT 1 FROM portal_account WHERE phone = '13351186381');
UPDATE portal_account
SET brewery_id = @brewery_miaoxiu,
    display_name = CONVERT(0xe79c87e4bfae USING utf8mb4),
    wechat = CONVERT(0x6666663133333531313836333831 USING utf8mb4),
    status = 1
WHERE phone = '13351186381';

-- 2. Reuse submitted styles and add the one style absent from this competition.
SET @style_czech_pils = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070
    AND name = CONVERT(0xe68db7e5858be79aaee5b094e6a3ae USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_international_lager = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070
    AND name = CONVERT(0xe59bbde99985e68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
INSERT INTO competition_style_config
  (competition_id, name, category_name, style_code, description,
   sort_order, active_flag, source_library_version)
SELECT 1070,
       CONVERT(0xe59bbde99985e68b89e6a0bc USING utf8mb4),
       CONVERT(0xe68aa5e5908de8a1a8e58e9fe5a78be9a38ee6a0bc USING utf8mb4),
       'IMPORT-1070-ADD-20260803-01',
       CONVERT(0xe69da5e6ba90efbc9ae9a696e5b18ae4b8ade59bbde68b89e6a0bce5a4a7e8b59be68aa5e5908de8a1a820323032362d30382d303320e5a29ee8a1a5 USING utf8mb4),
       1008,
       1,
       'REGISTRATION_FORM_20260803'
WHERE @style_international_lager IS NULL;
SET @style_international_lager = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070
    AND name = CONVERT(0xe59bbde99985e68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_international_amber = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070
    AND name = CONVERT(0xe59bbde99985e790a5e78f80e68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);

-- 3. Import entries and their active labels.
SET @entry_85079 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '00cd8a25-1c49-48d1-95f4-6290481685d0'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '00cd8a25-1c49-48d1-95f4-6290481685d0',
       1070, @brewery_cangshen, NULL, 1593,
       CONVERT(0xe8a5bfe69da5e6848f USING utf8mb4),
       CONVERT(0xe68db7e5858be79aaee5b094e6a3ae USING utf8mb4),
       @style_czech_pils,
       5.20,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),
       'REGISTERED', 0, 0
WHERE @entry_85079 IS NULL;
SET @entry_85079 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '00cd8a25-1c49-48d1-95f4-6290481685d0'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_85079, 'BE-IMP1070-ADD-AUG03-001', '85079',
       'Q1733DDEDC96F4DE9749BBE204A86B6179D9A5EBB2151D4993A1593B052198A1',
       'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_85079 AND status = 'ACTIVE'
);

SET @entry_55986 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = 'f6ce02aa-109c-4b09-8787-c0658f31f30a'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT 'f6ce02aa-109c-4b09-8787-c0658f31f30a',
       1070, @brewery_no_why, NULL, 1593,
       CONVERT(0xe68b89e6a0bcefbc9f USING utf8mb4),
       CONVERT(0xe59bbde99985e68b89e6a0bc USING utf8mb4),
       @style_international_lager,
       4.50,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),
       'REGISTERED', 0, 0
WHERE @entry_55986 IS NULL;
SET @entry_55986 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = 'f6ce02aa-109c-4b09-8787-c0658f31f30a'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_55986, 'BE-IMP1070-ADD-AUG03-002', '55986',
       'Q2CC7748124F071590C427F1C730EA02B1552B6E6C2F5E3D302A857B31373944',
       'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_55986 AND status = 'ACTIVE'
);

SET @entry_76015 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '134e7be3-b740-4912-9cc8-cec836dc1e26'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '134e7be3-b740-4912-9cc8-cec836dc1e26',
       1070, @brewery_miaoxiu, NULL, 1594,
       CONVERT(0xe7b396e78292e7b29fe5ad90 USING utf8mb4),
       CONVERT(0xe59bbde99985e790a5e78f80e68b89e6a0bc USING utf8mb4),
       @style_international_amber,
       5.00,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e590a6227d USING utf8mb4),
       'REGISTERED', 0, 0
WHERE @entry_76015 IS NULL;
SET @entry_76015 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '134e7be3-b740-4912-9cc8-cec836dc1e26'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_76015, 'BE-IMP1070-ADD-AUG03-003', '76015',
       'Q521871A42066EC90AA4E407309463D3CD9224458995D17076049C8765C86B20',
       'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_76015 AND status = 'ACTIVE'
);

-- 4. Normalize both configured extra fields for all three entries.
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT target.entry_id, config.field_key, config.field_label, CONVERT(0xe697a0 USING utf8mb4)
FROM (
  SELECT @entry_85079 entry_id UNION ALL
  SELECT @entry_55986 UNION ALL
  SELECT @entry_76015
) target
JOIN entry_field_config config
  ON config.competition_id = 1070
 AND config.field_key = 'specialIngredients'
 AND config.active_flag = 1
WHERE target.entry_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = target.entry_id
      AND existing.field_key = config.field_key
  );

INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT target.entry_id,
       config.field_key,
       config.field_label,
       CASE target.entry_id
         WHEN @entry_76015 THEN CONVERT(0xe590a6 USING utf8mb4)
         ELSE CONVERT(0xe698af USING utf8mb4)
       END
FROM (
  SELECT @entry_85079 entry_id UNION ALL
  SELECT @entry_55986 UNION ALL
  SELECT @entry_76015
) target
JOIN entry_field_config config
  ON config.competition_id = 1070
 AND config.field_key = 'custom_1785549677921_0089y'
 AND config.active_flag = 1
WHERE target.entry_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = target.entry_id
      AND existing.field_key = config.field_key
  );

COMMIT;

SELECT label.short_code, entry.name, brewery.company_name, brewery.contact_name,
       entry.style, entry.abv, label.status
FROM beer_entry entry
JOIN brewery brewery ON brewery.id = entry.brewery_id
JOIN entry_scan_label label ON label.beer_entry_id = entry.id
WHERE entry.competition_id = 1070
  AND entry.deleted_flag = 0
  AND label.short_code IN ('85079','55986','76015')
ORDER BY label.short_code;
