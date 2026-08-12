-- Roll back only the records introduced by the 2026-08-06 patch.
SET NAMES utf8mb4;
SET time_zone = '+08:00';
START TRANSACTION;

CREATE TEMPORARY TABLE tmp_rollback_1070_20260806_uuid (
  uuid CHAR(36) PRIMARY KEY
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
INSERT INTO tmp_rollback_1070_20260806_uuid VALUES
('0e2da54a-fb84-4a75-8595-4c7a847e3175'),
('de49a7e1-b3f5-4605-8e61-52e2b6b451b6'),
('62fcbc8c-dbfc-4bcc-8e00-c133103a6d6b'),
('85b9461b-fe98-499e-afee-e9df487a5ef4'),
('c571e76f-4373-4e98-81b8-2af863c12a78'),
('c02a1f23-6519-48ec-ad80-bb48ddeb8a8d'),
('72e699c9-c55b-4ff9-a2a6-efa4f3b77e32'),
('c2c38baf-a41f-4052-9533-cec8c458143f'),
('751c3d22-5f26-46e1-ba81-22d60d38e69e'),
('e00249ea-1687-4044-b27f-c08d9cd178fb'),
('26da345e-ce57-452c-bef4-886a78ebd2c2'),
('e1f97ca6-e815-49f0-a996-b542486499d2'),
('3455c0a1-66b6-4975-af32-db98a7a47d9d'),
('7cad99b3-b758-44b4-af03-e5a835b6f97a');

DELETE extra FROM beer_entry_extra_field extra
JOIN beer_entry entry ON entry.id = extra.beer_entry_id
JOIN tmp_rollback_1070_20260806_uuid target ON target.uuid = entry.uuid
WHERE entry.competition_id = 1070;

DELETE label FROM entry_scan_label label
JOIN beer_entry entry ON entry.id = label.beer_entry_id
JOIN tmp_rollback_1070_20260806_uuid target ON target.uuid = entry.uuid
WHERE entry.competition_id = 1070;

DELETE entry FROM beer_entry entry
JOIN tmp_rollback_1070_20260806_uuid target ON target.uuid = entry.uuid
WHERE entry.competition_id = 1070;

UPDATE brewery SET company_name = CONVERT(0xe5be85e5ae8ce59684e58e82e7898c33303030 USING utf8mb4),
  contact_name = CONVERT(0xe5be85e5ae8ce59684 USING utf8mb4), wechat = NULL
WHERE phone = '18643873000';
UPDATE portal_account SET display_name = CONVERT(0xe5be85e5ae8ce59684e58e82e7898c33303030 USING utf8mb4),
  wechat = NULL, status = 1 WHERE phone = '18643873000';

UPDATE brewery SET company_name = CONVERT(0xe5be85e5ae8ce59684e58e82e7898c31333032 USING utf8mb4),
  contact_name = CONVERT(0xe5be85e5ae8ce59684 USING utf8mb4), wechat = NULL
WHERE phone = '13584061302';
UPDATE portal_account SET display_name = CONVERT(0xe5be85e5ae8ce59684e58e82e7898c31333032 USING utf8mb4),
  wechat = NULL, status = 1 WHERE phone = '13584061302';

DELETE account FROM portal_account account
JOIN brewery brewery ON brewery.id = account.brewery_id
LEFT JOIN beer_entry entry ON entry.brewery_id = brewery.id
WHERE account.phone = '18802771906'
  AND brewery.company_name = CONVERT(0xe68890e983bde58d97e997a8e7b2bee985bf USING utf8mb4) AND entry.id IS NULL;

DELETE brewery FROM brewery brewery
LEFT JOIN beer_entry entry ON entry.brewery_id = brewery.id
LEFT JOIN portal_account account ON account.brewery_id = brewery.id
WHERE brewery.phone = '18802771906'
  AND brewery.company_name = CONVERT(0xe68890e983bde58d97e997a8e7b2bee985bf USING utf8mb4)
  AND entry.id IS NULL AND account.id IS NULL;

DELETE style FROM competition_style_config style
LEFT JOIN beer_entry entry ON entry.style_config_id = style.id
WHERE style.competition_id = 1070
  AND style.source_library_version = 'REGISTRATION_FORM_20260806'
  AND style.style_code LIKE 'IMPORT-1070-ADD-20260806-%'
  AND entry.id IS NULL;

COMMIT;

SELECT 'competition_entries_after_rollback' check_item, COUNT(*) value
FROM beer_entry WHERE competition_id = 1070 AND deleted_flag = 0;
SELECT 'targets_after_rollback' check_item, COUNT(*) value
FROM entry_scan_label WHERE competition_id = 1070 AND short_code IN ('22091','22092','22093','22094','22095','22096','22097','49381','49382','49383','49384','89622','43113','48819');
