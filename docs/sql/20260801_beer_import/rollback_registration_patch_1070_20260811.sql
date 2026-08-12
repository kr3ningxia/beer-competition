-- Roll back only the records introduced by the 2026-08-08/11 patch.
SET NAMES utf8mb4;
SET time_zone='+08:00';
START TRANSACTION;
CREATE TEMPORARY TABLE tmp_rollback_1070_20260811_uuid (uuid CHAR(36) PRIMARY KEY) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
INSERT INTO tmp_rollback_1070_20260811_uuid VALUES
('af001b8a-b426-4a5d-b49b-868355fd4134'),
('32ea5806-6ef3-40db-a926-dedaa72e16cf'),
('e91a526c-4e54-4bd8-b578-f4a281f172e4'),
('46d272b3-e57f-45ec-b00b-f436fd22e9ee'),
('872bcded-b72d-41bf-b6a2-341dde50c25c'),
('9f72fb5e-b34a-4ae7-810d-cce39dd3a029'),
('cb63c771-0342-4e57-b4dc-ed6a2a792882');
DELETE extra FROM beer_entry_extra_field extra JOIN beer_entry entry ON entry.id=extra.beer_entry_id JOIN tmp_rollback_1070_20260811_uuid target ON target.uuid=entry.uuid WHERE entry.competition_id=1070;
DELETE label FROM entry_scan_label label JOIN beer_entry entry ON entry.id=label.beer_entry_id JOIN tmp_rollback_1070_20260811_uuid target ON target.uuid=entry.uuid WHERE entry.competition_id=1070;
DELETE entry FROM beer_entry entry JOIN tmp_rollback_1070_20260811_uuid target ON target.uuid=entry.uuid WHERE entry.competition_id=1070;
UPDATE brewery SET wechat='fulijilu' WHERE phone='15954545181';
UPDATE portal_account SET wechat='fulijilu' WHERE phone='15954545181';
DELETE account FROM portal_account account JOIN brewery brewery ON brewery.id=account.brewery_id LEFT JOIN beer_entry entry ON entry.brewery_id=brewery.id WHERE account.phone='18208851300' AND brewery.company_name=CONVERT(0xe69dafe590bee985bfe980a0 USING utf8mb4) AND entry.id IS NULL;
DELETE brewery FROM brewery brewery LEFT JOIN beer_entry entry ON entry.brewery_id=brewery.id LEFT JOIN portal_account account ON account.brewery_id=brewery.id WHERE brewery.phone='18208851300' AND brewery.company_name=CONVERT(0xe69dafe590bee985bfe980a0 USING utf8mb4) AND entry.id IS NULL AND account.id IS NULL;
DELETE style FROM competition_style_config style LEFT JOIN beer_entry entry ON entry.style_config_id=style.id WHERE style.competition_id=1070 AND style.source_library_version='REGISTRATION_FORM_20260808_20260811' AND style.style_code LIKE 'IMPORT-1070-ADD-20260811-%' AND entry.id IS NULL;
COMMIT;
SELECT 'competition_entries_after_rollback' check_item,COUNT(*) value FROM beer_entry WHERE competition_id=1070 AND deleted_flag=0;
SELECT 'targets_after_rollback' check_item,COUNT(*) value FROM entry_scan_label WHERE competition_id=1070 AND short_code IN ('54531','54532','54533','54534','62884','55190','81451');
SELECT 'styles_after_rollback' check_item,COUNT(*) value FROM competition_style_config WHERE competition_id=1070 AND source_library_version='REGISTRATION_FORM_20260808_20260811';
