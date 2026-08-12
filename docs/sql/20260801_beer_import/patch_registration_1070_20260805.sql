-- Competition 1070: import 13 entries submitted on 2026-08-04 and 2026-08-05.
SET NAMES utf8mb4;
SET time_zone = '+08:00';
START TRANSACTION;

-- Abort before changes if a target short code belongs to another entry.
SET @target_code_conflicts = (
  SELECT COUNT(*)
  FROM entry_scan_label label
  JOIN beer_entry entry ON entry.id = label.beer_entry_id
  WHERE label.competition_id = 1070
    AND label.short_code IN ('31821','31822','31823','31824','31825','32301','32302','32303','32304','32305','11861','11862','11863')
    AND entry.uuid NOT IN ('18397dc4-9d92-48b7-8e46-b9ceb0cd229e','d28ea3c7-9c7d-4d77-afe7-22d1e975f60d','9c922d67-eb90-4c50-b646-94d316dc4e60','14f73eef-9d07-4803-a8c3-3b3498aa7580','6f7d12ca-1efa-4bef-9250-02f0fd80a361','906ec512-9af2-40f1-938e-c8af81934e43','59019f95-a88d-48bf-a035-be25a87e1876','98975aec-76e1-4aaa-a373-844d463c616d','8256a40b-44a5-450c-afc8-140f69e44833','9edeb6ab-db68-4e3b-816a-a2641fe408fe','4f6ef47c-2001-4156-b200-de414246c08c','de853273-7299-4b26-b755-63734eac8883','86343048-bf4a-4acd-93dc-94926a20a431')
);
SELECT CASE WHEN @target_code_conflicts = 0 THEN 1 ELSE 1 / 0 END INTO @target_codes_available;

-- Brewery and portal account: DADODO.
SET @brewery_dadodo = (
  SELECT MIN(b.id)
  FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id = b.id
  WHERE a.phone = '13917195955' OR b.phone = '13917195955'
     OR b.company_name = CONVERT(0x4441444f444f USING utf8mb4)
);
INSERT INTO brewery (company_name, contact_name, phone, wechat)
SELECT CONVERT(0x4441444f444f USING utf8mb4), CONVERT(0xe69ca8e69ca8 USING utf8mb4), '13917195955', CONVERT(0x3133393137313935393535 USING utf8mb4)
WHERE @brewery_dadodo IS NULL;
SET @brewery_dadodo = (
  SELECT MIN(b.id)
  FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id = b.id
  WHERE a.phone = '13917195955' OR b.phone = '13917195955'
     OR b.company_name = CONVERT(0x4441444f444f USING utf8mb4)
);
UPDATE brewery
SET company_name = CONVERT(0x4441444f444f USING utf8mb4),
    contact_name = CONVERT(0xe69ca8e69ca8 USING utf8mb4),
    phone = '13917195955',
    wechat = CONVERT(0x3133393137313935393535 USING utf8mb4)
WHERE id = @brewery_dadodo;
INSERT INTO portal_account (phone, wechat, display_name, brewery_id, status)
SELECT '13917195955', CONVERT(0x3133393137313935393535 USING utf8mb4), CONVERT(0x4441444f444f USING utf8mb4), @brewery_dadodo, 1
WHERE NOT EXISTS (SELECT 1 FROM portal_account WHERE phone = '13917195955');
UPDATE portal_account
SET brewery_id = @brewery_dadodo, display_name = CONVERT(0x4441444f444f USING utf8mb4),
    wechat = CONVERT(0x3133393137313935393535 USING utf8mb4), status = 1
WHERE phone = '13917195955';

-- Brewery and portal account: 香格里拉啤酒.
SET @brewery_shangrila = (
  SELECT MIN(b.id)
  FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id = b.id
  WHERE a.phone = '18308877948' OR b.phone = '18308877948'
     OR b.company_name = CONVERT(0xe9a699e6a0bce9878ce68b89e595a4e98592 USING utf8mb4)
);
INSERT INTO brewery (company_name, contact_name, phone, wechat)
SELECT CONVERT(0xe9a699e6a0bce9878ce68b89e595a4e98592 USING utf8mb4), CONVERT(0xe58d93e78e9be68b89e5a786 USING utf8mb4), '18308877948', CONVERT(0x3138333038383737393438 USING utf8mb4)
WHERE @brewery_shangrila IS NULL;
SET @brewery_shangrila = (
  SELECT MIN(b.id)
  FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id = b.id
  WHERE a.phone = '18308877948' OR b.phone = '18308877948'
     OR b.company_name = CONVERT(0xe9a699e6a0bce9878ce68b89e595a4e98592 USING utf8mb4)
);
UPDATE brewery
SET company_name = CONVERT(0xe9a699e6a0bce9878ce68b89e595a4e98592 USING utf8mb4),
    contact_name = CONVERT(0xe58d93e78e9be68b89e5a786 USING utf8mb4),
    phone = '18308877948',
    wechat = CONVERT(0x3138333038383737393438 USING utf8mb4)
WHERE id = @brewery_shangrila;
INSERT INTO portal_account (phone, wechat, display_name, brewery_id, status)
SELECT '18308877948', CONVERT(0x3138333038383737393438 USING utf8mb4), CONVERT(0xe9a699e6a0bce9878ce68b89e595a4e98592 USING utf8mb4), @brewery_shangrila, 1
WHERE NOT EXISTS (SELECT 1 FROM portal_account WHERE phone = '18308877948');
UPDATE portal_account
SET brewery_id = @brewery_shangrila, display_name = CONVERT(0xe9a699e6a0bce9878ce68b89e595a4e98592 USING utf8mb4),
    wechat = CONVERT(0x3138333038383737393438 USING utf8mb4), status = 1
WHERE phone = '18308877948';

-- Brewery and portal account: 有哈数.
SET @brewery_youhashu = (
  SELECT MIN(b.id)
  FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id = b.id
  WHERE a.phone = '13341305088' OR b.phone = '13341305088'
     OR b.company_name = CONVERT(0xe69c89e59388e695b0 USING utf8mb4)
);
INSERT INTO brewery (company_name, contact_name, phone, wechat)
SELECT CONVERT(0xe69c89e59388e695b0 USING utf8mb4), CONVERT(0xe998bfe6a8b5 USING utf8mb4), '13341305088', CONVERT(0x3333363737383533 USING utf8mb4)
WHERE @brewery_youhashu IS NULL;
SET @brewery_youhashu = (
  SELECT MIN(b.id)
  FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id = b.id
  WHERE a.phone = '13341305088' OR b.phone = '13341305088'
     OR b.company_name = CONVERT(0xe69c89e59388e695b0 USING utf8mb4)
);
UPDATE brewery
SET company_name = CONVERT(0xe69c89e59388e695b0 USING utf8mb4),
    contact_name = CONVERT(0xe998bfe6a8b5 USING utf8mb4),
    phone = '13341305088',
    wechat = CONVERT(0x3333363737383533 USING utf8mb4)
WHERE id = @brewery_youhashu;
INSERT INTO portal_account (phone, wechat, display_name, brewery_id, status)
SELECT '13341305088', CONVERT(0x3333363737383533 USING utf8mb4), CONVERT(0xe69c89e59388e695b0 USING utf8mb4), @brewery_youhashu, 1
WHERE NOT EXISTS (SELECT 1 FROM portal_account WHERE phone = '13341305088');
UPDATE portal_account
SET brewery_id = @brewery_youhashu, display_name = CONVERT(0xe69c89e59388e695b0 USING utf8mb4),
    wechat = CONVERT(0x3333363737383533 USING utf8mb4), status = 1
WHERE phone = '13341305088';

-- Reuse existing competition styles and add only missing form-specific styles.
SET @style_style_01baf63ecc24 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe6848fe5bc8fe79aaee5b094e6a3ae USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
INSERT INTO competition_style_config
  (competition_id, name, category_name, style_code, description,
   sort_order, active_flag, source_library_version)
SELECT 1070, CONVERT(0xe6848fe5bc8fe79aaee5b094e6a3ae USING utf8mb4), CONVERT(0xe68aa5e5908de8a1a8e887aae5a1abe59fbae7a180e9a38ee6a0bc USING utf8mb4),
       'IMPORT-1070-ADD-20260805-01',
       CONVERT(0xe69da5e6ba90efbc9ae9a696e5b18ae4b8ade59bbde68b89e6a0bce5a4a7e8b59be68aa5e5908de8a1a820323032362d30382d30342f303520e696b0e5a29e USING utf8mb4), 1009, 1, 'REGISTRATION_FORM_20260805'
WHERE @style_style_01baf63ecc24 IS NULL;
SET @style_style_01baf63ecc24 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe6848fe5bc8fe79aaee5b094e6a3ae USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_style_a015e0830e8f = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe5beb7e5bc8fe79aaee5b094e6a3ae USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_style_165dfb8d74bf = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe68db7e5858be79aaee5b094e6a3ae USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_style_c00e5f7c3f88 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe9bb91e889b2e68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
INSERT INTO competition_style_config
  (competition_id, name, category_name, style_code, description,
   sort_order, active_flag, source_library_version)
SELECT 1070, CONVERT(0xe9bb91e889b2e68b89e6a0bc USING utf8mb4), CONVERT(0xe68aa5e5908de8a1a8e887aae5a1abe59fbae7a180e9a38ee6a0bc USING utf8mb4),
       'IMPORT-1070-ADD-20260805-02',
       CONVERT(0xe69da5e6ba90efbc9ae9a696e5b18ae4b8ade59bbde68b89e6a0bce5a4a7e8b59be68aa5e5908de8a1a820323032362d30382d30342f303520e696b0e5a29e USING utf8mb4), 1010, 1, 'REGISTRATION_FORM_20260805'
WHERE @style_style_c00e5f7c3f88 IS NULL;
SET @style_style_c00e5f7c3f88 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe9bb91e889b2e68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_style_d8f4d9bb1332 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe6b7a1e68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
INSERT INTO competition_style_config
  (competition_id, name, category_name, style_code, description,
   sort_order, active_flag, source_library_version)
SELECT 1070, CONVERT(0xe6b7a1e68b89e6a0bc USING utf8mb4), CONVERT(0xe68aa5e5908de8a1a8e887aae5a1abe59fbae7a180e9a38ee6a0bc USING utf8mb4),
       'IMPORT-1070-ADD-20260805-03',
       CONVERT(0xe69da5e6ba90efbc9ae9a696e5b18ae4b8ade59bbde68b89e6a0bce5a4a7e8b59be68aa5e5908de8a1a820323032362d30382d30342f303520e696b0e5a29e USING utf8mb4), 1011, 1, 'REGISTRATION_FORM_20260805'
WHERE @style_style_d8f4d9bb1332 IS NULL;
SET @style_style_d8f4d9bb1332 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe6b7a1e68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_style_4c347cf0626d = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe5beb7e5bc8fe68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_style_8530e6a05666 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe8b4a1e7b1b3e68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
INSERT INTO competition_style_config
  (competition_id, name, category_name, style_code, description,
   sort_order, active_flag, source_library_version)
SELECT 1070, CONVERT(0xe8b4a1e7b1b3e68b89e6a0bc USING utf8mb4), CONVERT(0xe68aa5e5908de8a1a8e887aae5a1abe59fbae7a180e9a38ee6a0bc USING utf8mb4),
       'IMPORT-1070-ADD-20260805-04',
       CONVERT(0xe69da5e6ba90efbc9ae9a696e5b18ae4b8ade59bbde68b89e6a0bce5a4a7e8b59be68aa5e5908de8a1a820323032362d30382d30342f303520e696b0e5a29e USING utf8mb4), 1012, 1, 'REGISTRATION_FORM_20260805'
WHERE @style_style_8530e6a05666 IS NULL;
SET @style_style_8530e6a05666 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe8b4a1e7b1b3e68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_style_0ce403afde14 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe79aaee5b094e6a3ae USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_style_bbebb28ed533 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe7b2bee7b1b3e68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
INSERT INTO competition_style_config
  (competition_id, name, category_name, style_code, description,
   sort_order, active_flag, source_library_version)
SELECT 1070, CONVERT(0xe7b2bee7b1b3e68b89e6a0bc USING utf8mb4), CONVERT(0xe68aa5e5908de8a1a8e887aae5a1abe59fbae7a180e9a38ee6a0bc USING utf8mb4),
       'IMPORT-1070-ADD-20260805-05',
       CONVERT(0xe69da5e6ba90efbc9ae9a696e5b18ae4b8ade59bbde68b89e6a0bce5a4a7e8b59be68aa5e5908de8a1a820323032362d30382d30342f303520e696b0e5a29e USING utf8mb4), 1013, 1, 'REGISTRATION_FORM_20260805'
WHERE @style_style_bbebb28ed533 IS NULL;
SET @style_style_bbebb28ed533 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe7b2bee7b1b3e68b89e6a0bc USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_style_5e8304eac116 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe69fa0e6aaace79aaee5b094e6a3ae USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
INSERT INTO competition_style_config
  (competition_id, name, category_name, style_code, description,
   sort_order, active_flag, source_library_version)
SELECT 1070, CONVERT(0xe69fa0e6aaace79aaee5b094e6a3ae USING utf8mb4), CONVERT(0xe68aa5e5908de8a1a8e887aae5a1abe59fbae7a180e9a38ee6a0bc USING utf8mb4),
       'IMPORT-1070-ADD-20260805-06',
       CONVERT(0xe69da5e6ba90efbc9ae9a696e5b18ae4b8ade59bbde68b89e6a0bce5a4a7e8b59be68aa5e5908de8a1a820323032362d30382d30342f303520e696b0e5a29e USING utf8mb4), 1014, 1, 'REGISTRATION_FORM_20260805'
WHERE @style_style_5e8304eac116 IS NULL;
SET @style_style_5e8304eac116 = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe69fa0e6aaace79aaee5b094e6a3ae USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);
SET @style_style_ccdf8cb1360d = (
  SELECT id FROM competition_style_config
  WHERE competition_id = 1070 AND name = CONVERT(0xe98592e88ab1e79aaee5b094e6a3ae USING utf8mb4)
  ORDER BY id DESC LIMIT 1
);

-- Import entries, active scan labels and both configured extra fields.
SET @entry_31821 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '18397dc4-9d92-48b7-8e46-b9ceb0cd229e'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '18397dc4-9d92-48b7-8e46-b9ceb0cd229e', 1070, @brewery_dadodo, NULL, 1593,
       CONVERT(0xe590a8e590a8e79aaee5b094e6a3ae USING utf8mb4), CONVERT(0xe6848fe5bc8fe79aaee5b094e6a3ae USING utf8mb4), @style_style_01baf63ecc24, 5.10,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_31821 IS NULL;
SET @entry_31821 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '18397dc4-9d92-48b7-8e46-b9ceb0cd229e'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_31821, 'BE-IMP1070-ADD-AUG0405-001',
       '31821', 'Q460BD97C779AA4C7AC77B670477B07DEFADC5070D2FB2887037E95BCCCBFC06', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_31821 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_31821, config.field_key, config.field_label, CONVERT(0xe697a0 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_31821
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_31821, config.field_key, config.field_label, CONVERT(0xe698af USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_31821
      AND existing.field_key = config.field_key
  );

SET @entry_31822 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = 'd28ea3c7-9c7d-4d77-afe7-22d1e975f60d'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT 'd28ea3c7-9c7d-4d77-afe7-22d1e975f60d', 1070, @brewery_dadodo, NULL, 1593,
       CONVERT(0xe590a8e590a8e5b08fe9baa6 USING utf8mb4), CONVERT(0xe5beb7e5bc8fe79aaee5b094e6a3ae USING utf8mb4), @style_style_a015e0830e8f, 5.00,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_31822 IS NULL;
SET @entry_31822 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = 'd28ea3c7-9c7d-4d77-afe7-22d1e975f60d'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_31822, 'BE-IMP1070-ADD-AUG0405-002',
       '31822', 'QDEE9EFBDC16596908E0A8B466E36BA2D0804E3D2F9DDAF8B688C54D1E58701D', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_31822 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_31822, config.field_key, config.field_label, CONVERT(0xe697a0 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_31822
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_31822, config.field_key, config.field_label, CONVERT(0xe698af USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_31822
      AND existing.field_key = config.field_key
  );

SET @entry_31823 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '9c922d67-eb90-4c50-b646-94d316dc4e60'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '9c922d67-eb90-4c50-b646-94d316dc4e60', 1070, @brewery_dadodo, NULL, 1593,
       CONVERT(0xe590a8e590a8e5a4a7e9baa6 USING utf8mb4), CONVERT(0xe68db7e5858be79aaee5b094e6a3ae USING utf8mb4), @style_style_165dfb8d74bf, 5.00,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_31823 IS NULL;
SET @entry_31823 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '9c922d67-eb90-4c50-b646-94d316dc4e60'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_31823, 'BE-IMP1070-ADD-AUG0405-003',
       '31823', 'QD066E239E948E763F0B934688FC2B12E0F921D91942B30FD3ABE70D0538EF68', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_31823 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_31823, config.field_key, config.field_label, CONVERT(0xe697a0 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_31823
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_31823, config.field_key, config.field_label, CONVERT(0xe698af USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_31823
      AND existing.field_key = config.field_key
  );

SET @entry_31824 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '14f73eef-9d07-4803-a8c3-3b3498aa7580'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '14f73eef-9d07-4803-a8c3-3b3498aa7580', 1070, @brewery_dadodo, NULL, 1593,
       CONVERT(0xe5a4a7e9baa6e58e9fe6b586 USING utf8mb4), CONVERT(0xe5beb7e5bc8fe79aaee5b094e6a3ae USING utf8mb4), @style_style_a015e0830e8f, 4.10,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_31824 IS NULL;
SET @entry_31824 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '14f73eef-9d07-4803-a8c3-3b3498aa7580'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_31824, 'BE-IMP1070-ADD-AUG0405-004',
       '31824', 'Q5AD1BC2AF4CA9D770D83F2CB990B7A3B07D98DB5A6BA6EBBA20438B08C5CB8A', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_31824 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_31824, config.field_key, config.field_label, CONVERT(0xe697a0 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_31824
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_31824, config.field_key, config.field_label, CONVERT(0xe698af USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_31824
      AND existing.field_key = config.field_key
  );

SET @entry_31825 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '6f7d12ca-1efa-4bef-9250-02f0fd80a361'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '6f7d12ca-1efa-4bef-9250-02f0fd80a361', 1070, @brewery_dadodo, NULL, 1595,
       CONVERT(0xe590a8e590a8e9bb91e595a4 USING utf8mb4), CONVERT(0xe9bb91e889b2e68b89e6a0bc USING utf8mb4), @style_style_c00e5f7c3f88, 4.00,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e59296e595a1e89083e58f96e6b6b2222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_31825 IS NULL;
SET @entry_31825 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '6f7d12ca-1efa-4bef-9250-02f0fd80a361'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_31825, 'BE-IMP1070-ADD-AUG0405-005',
       '31825', 'Q088CCA79912F1E0C6C2E39850B6C3A3A7AD819830D8C4D52E1840CA1D4CBD21', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_31825 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_31825, config.field_key, config.field_label, CONVERT(0xe59296e595a1e89083e58f96e6b6b2 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_31825
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_31825, config.field_key, config.field_label, CONVERT(0xe698af USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_31825
      AND existing.field_key = config.field_key
  );

SET @entry_32301 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '906ec512-9af2-40f1-938e-c8af81934e43'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '906ec512-9af2-40f1-938e-c8af81934e43', 1070, @brewery_shangrila, NULL, 1593,
       CONVERT(0xe597a6e59180e595a6536f59614c61 USING utf8mb4), CONVERT(0xe6b7a1e68b89e6a0bc USING utf8mb4), @style_style_d8f4d9bb1332, 3.00,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_32301 IS NULL;
SET @entry_32301 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '906ec512-9af2-40f1-938e-c8af81934e43'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_32301, 'BE-IMP1070-ADD-AUG0405-006',
       '32301', 'Q032148893BDF38A84AC570D8138CBA7EBE90882FB524147995F548F605CC518', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_32301 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_32301, config.field_key, config.field_label, CONVERT(0xe697a0 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_32301
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_32301, config.field_key, config.field_label, CONVERT(0xe698af USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_32301
      AND existing.field_key = config.field_key
  );

SET @entry_32302 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '59019f95-a88d-48bf-a035-be25a87e1876'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '59019f95-a88d-48bf-a035-be25a87e1876', 1070, @brewery_shangrila, NULL, 1594,
       CONVERT(0xe69dbee5988e536f6e676861 USING utf8mb4), CONVERT(0xe5beb7e5bc8fe68b89e6a0bc USING utf8mb4), @style_style_4c347cf0626d, 5.20,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_32302 IS NULL;
SET @entry_32302 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '59019f95-a88d-48bf-a035-be25a87e1876'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_32302, 'BE-IMP1070-ADD-AUG0405-007',
       '32302', 'Q244EFEF65EF5AA6278DF765031B47100D6BE2086EAF43F7A19DC76A8C7EF26C', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_32302 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_32302, config.field_key, config.field_label, CONVERT(0xe697a0 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_32302
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_32302, config.field_key, config.field_label, CONVERT(0xe698af USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_32302
      AND existing.field_key = config.field_key
  );

SET @entry_32303 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '98975aec-76e1-4aaa-a373-844d463c616d'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '98975aec-76e1-4aaa-a373-844d463c616d', 1070, @brewery_shangrila, NULL, 1595,
       CONVERT(0xe78c9be78495e7949fe595a4 USING utf8mb4), CONVERT(0xe8b4a1e7b1b3e68b89e6a0bc USING utf8mb4), @style_style_8530e6a05666, 5.20,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e6b7bbe58aa0e5beb7e5ae8fe88a92e5b882e8b4a1e7b1b3222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_32303 IS NULL;
SET @entry_32303 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '98975aec-76e1-4aaa-a373-844d463c616d'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_32303, 'BE-IMP1070-ADD-AUG0405-008',
       '32303', 'Q3FCDCDEF509994CE28E7B44DDFDC06D0C42E13596A510D48F2B9ADD7FF45DA3', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_32303 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_32303, config.field_key, config.field_label, CONVERT(0xe6b7bbe58aa0e5beb7e5ae8fe88a92e5b882e8b4a1e7b1b3 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_32303
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_32303, config.field_key, config.field_label, CONVERT(0xe698af USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_32303
      AND existing.field_key = config.field_key
  );

SET @entry_32304 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '8256a40b-44a5-450c-afc8-140f69e44833'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '8256a40b-44a5-450c-afc8-140f69e44833', 1070, @brewery_shangrila, NULL, 1595,
       CONVERT(0xe9a699e6a0bce9878ce68b89e7949fe595a4 USING utf8mb4), CONVERT(0xe8b4a1e7b1b3e68b89e6a0bc USING utf8mb4), @style_style_8530e6a05666, 5.20,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e6b7bbe58aa0e5beb7e5ae8fe88a92e5b882e8b4a1e7b1b3222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_32304 IS NULL;
SET @entry_32304 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '8256a40b-44a5-450c-afc8-140f69e44833'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_32304, 'BE-IMP1070-ADD-AUG0405-009',
       '32304', 'QA03BB6FE977A07890EB3F7C69E91AE5BA9F5BED2861CD8BF2B9D8241C5AEA4E', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_32304 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_32304, config.field_key, config.field_label, CONVERT(0xe6b7bbe58aa0e5beb7e5ae8fe88a92e5b882e8b4a1e7b1b3 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_32304
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_32304, config.field_key, config.field_label, CONVERT(0xe698af USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_32304
      AND existing.field_key = config.field_key
  );

SET @entry_32305 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '9edeb6ab-db68-4e3b-816a-a2641fe408fe'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '9edeb6ab-db68-4e3b-816a-a2641fe408fe', 1070, @brewery_shangrila, NULL, 1595,
       CONVERT(0xe9a699e6a0bce9878ce68b89e8b685e5b9b2e7949fe595a4 USING utf8mb4), CONVERT(0xe79aaee5b094e6a3ae USING utf8mb4), @style_style_0ce403afde14, 4.20,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e6b7bbe58aa0e4ba86e5beb7e5ae8fe88a92e5b882e8b4a1e7b1b3222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_32305 IS NULL;
SET @entry_32305 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '9edeb6ab-db68-4e3b-816a-a2641fe408fe'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_32305, 'BE-IMP1070-ADD-AUG0405-010',
       '32305', 'QD58F6F50F96C9F4029189286C4CA60C69187FA9949A2685B84EBBA4AC08F115', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_32305 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_32305, config.field_key, config.field_label, CONVERT(0xe6b7bbe58aa0e4ba86e5beb7e5ae8fe88a92e5b882e8b4a1e7b1b3 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_32305
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_32305, config.field_key, config.field_label, CONVERT(0xe698af USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_32305
      AND existing.field_key = config.field_key
  );

SET @entry_11861 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '4f6ef47c-2001-4156-b200-de414246c08c'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '4f6ef47c-2001-4156-b200-de414246c08c', 1070, @brewery_youhashu, NULL, 1595,
       CONVERT(0xe6b998e58d97e6999ae9998c USING utf8mb4), CONVERT(0xe7b2bee7b1b3e68b89e6a0bc USING utf8mb4), @style_style_bbebb28ed533, 4.10,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e590a6227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_11861 IS NULL;
SET @entry_11861 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '4f6ef47c-2001-4156-b200-de414246c08c'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_11861, 'BE-IMP1070-ADD-AUG0405-011',
       '11861', 'QFFF7F61C0A8A0818540A6332781B7A9D5D5BE979927102BFDE1EF234237CC31', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_11861 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_11861, config.field_key, config.field_label, CONVERT(0xe697a0 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_11861
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_11861, config.field_key, config.field_label, CONVERT(0xe590a6 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_11861
      AND existing.field_key = config.field_key
  );

SET @entry_11862 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = 'de853273-7299-4b26-b755-63734eac8883'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT 'de853273-7299-4b26-b755-63734eac8883', 1070, @brewery_youhashu, NULL, 1595,
       CONVERT(0xe6ad87e58789 USING utf8mb4), CONVERT(0xe69fa0e6aaace79aaee5b094e6a3ae USING utf8mb4), @style_style_5e8304eac116, 4.10,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e69fa0e6aaace79aaee4b88ee99d92e69fa0e6b181222c22637573746f6d5f313738353534393637373932315f3030383979223a22e590a6227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_11862 IS NULL;
SET @entry_11862 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = 'de853273-7299-4b26-b755-63734eac8883'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_11862, 'BE-IMP1070-ADD-AUG0405-012',
       '11862', 'Q23ED7058C8B3FE90DC097E4A95A110779BB3F7105049BC9FF54B3403CB44809', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_11862 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_11862, config.field_key, config.field_label, CONVERT(0xe69fa0e6aaace79aaee4b88ee99d92e69fa0e6b181 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_11862
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_11862, config.field_key, config.field_label, CONVERT(0xe590a6 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_11862
      AND existing.field_key = config.field_key
  );

SET @entry_11863 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '86343048-bf4a-4acd-93dc-94926a20a431'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json,
   status, stored_flag, deleted_flag)
SELECT '86343048-bf4a-4acd-93dc-94926a20a431', 1070, @brewery_youhashu, NULL, 1593,
       CONVERT(0xe6bdaee6ada2 USING utf8mb4), CONVERT(0xe98592e88ab1e79aaee5b094e6a3ae USING utf8mb4), @style_style_ccdf8cb1360d, 4.10,
       CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e590a6227d USING utf8mb4), 'REGISTERED', 0, 0
WHERE @entry_11863 IS NULL;
SET @entry_11863 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '86343048-bf4a-4acd-93dc-94926a20a431'
    AND deleted_flag = 0 LIMIT 1
);
INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, @entry_11863, 'BE-IMP1070-ADD-AUG0405-013',
       '11863', 'QE64EC71D77E46BE3F25FA7AC0C6B025D91236D8E2158D2C3AA2D35067DA7A85', 'ACTIVE', NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label
  WHERE beer_entry_id = @entry_11863 AND status = 'ACTIVE'
);
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_11863, config.field_key, config.field_label, CONVERT(0xe697a0 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_11863
      AND existing.field_key = config.field_key
  );
INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT @entry_11863, config.field_key, config.field_label, CONVERT(0xe590a6 USING utf8mb4)
FROM entry_field_config config
WHERE config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM beer_entry_extra_field existing
    WHERE existing.beer_entry_id = @entry_11863
      AND existing.field_key = config.field_key
  );

COMMIT;

SELECT label.short_code, entry.name, brewery.company_name, entry.style, entry.abv, label.status
FROM beer_entry entry
JOIN brewery brewery ON brewery.id = entry.brewery_id
JOIN entry_scan_label label ON label.beer_entry_id = entry.id
WHERE entry.competition_id = 1070
  AND entry.deleted_flag = 0
  AND label.short_code IN ('31821','31822','31823','31824','31825','32301','32302','32303','32304','32305','11861','11862','11863')
ORDER BY label.short_code;

