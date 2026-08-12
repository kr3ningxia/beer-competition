SET NAMES utf8mb4;
SET time_zone='+08:00';
START TRANSACTION;
CREATE TEMPORARY TABLE tmp_rollback_1070_20260807_uuid (uuid CHAR(36) PRIMARY KEY) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
INSERT INTO tmp_rollback_1070_20260807_uuid VALUES
('a181e561-f2dd-4f9c-82d2-66e48b35d44f'),
('4f2ce5f1-102c-45ce-9922-7579ea6eca3d'),
('b3424882-ef67-4d7c-abe2-1dac281c3e09'),
('1d6abf78-9ffc-45f6-9178-a4b5ed48fb73');
DELETE extra FROM beer_entry_extra_field extra JOIN beer_entry entry ON entry.id=extra.beer_entry_id JOIN tmp_rollback_1070_20260807_uuid target ON target.uuid=entry.uuid WHERE entry.competition_id=1070;
DELETE label FROM entry_scan_label label JOIN beer_entry entry ON entry.id=label.beer_entry_id JOIN tmp_rollback_1070_20260807_uuid target ON target.uuid=entry.uuid WHERE entry.competition_id=1070;
DELETE entry FROM beer_entry entry JOIN tmp_rollback_1070_20260807_uuid target ON target.uuid=entry.uuid WHERE entry.competition_id=1070;
DELETE style FROM competition_style_config style LEFT JOIN beer_entry entry ON entry.style_config_id=style.id WHERE style.competition_id=1070 AND style.source_library_version='REGISTRATION_FORM_20260807' AND style.style_code='IMPORT-1070-ADD-20260807-01' AND entry.id IS NULL;
COMMIT;
SELECT 'competition_entries_after_rollback' check_item,COUNT(*) value FROM beer_entry WHERE competition_id=1070 AND deleted_flag=0;
SELECT 'targets_after_rollback' check_item,COUNT(*) value FROM entry_scan_label WHERE competition_id=1070 AND short_code IN ('93641','93642','93643','93644');

