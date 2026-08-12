-- Roll back only the records introduced by the 2026-08-04/05 patch.
SET NAMES utf8mb4;
SET time_zone = '+08:00';
START TRANSACTION;

SET @entry_31821 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = '18397dc4-9d92-48b7-8e46-b9ceb0cd229e' LIMIT 1);
SET @entry_31822 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = 'd28ea3c7-9c7d-4d77-afe7-22d1e975f60d' LIMIT 1);
SET @entry_31823 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = '9c922d67-eb90-4c50-b646-94d316dc4e60' LIMIT 1);
SET @entry_31824 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = '14f73eef-9d07-4803-a8c3-3b3498aa7580' LIMIT 1);
SET @entry_31825 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = '6f7d12ca-1efa-4bef-9250-02f0fd80a361' LIMIT 1);
SET @entry_32301 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = '906ec512-9af2-40f1-938e-c8af81934e43' LIMIT 1);
SET @entry_32302 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = '59019f95-a88d-48bf-a035-be25a87e1876' LIMIT 1);
SET @entry_32303 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = '98975aec-76e1-4aaa-a373-844d463c616d' LIMIT 1);
SET @entry_32304 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = '8256a40b-44a5-450c-afc8-140f69e44833' LIMIT 1);
SET @entry_32305 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = '9edeb6ab-db68-4e3b-816a-a2641fe408fe' LIMIT 1);
SET @entry_11861 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = '4f6ef47c-2001-4156-b200-de414246c08c' LIMIT 1);
SET @entry_11862 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = 'de853273-7299-4b26-b755-63734eac8883' LIMIT 1);
SET @entry_11863 = (SELECT id FROM beer_entry WHERE competition_id = 1070 AND uuid = '86343048-bf4a-4acd-93dc-94926a20a431' LIMIT 1);

DELETE FROM beer_entry_extra_field WHERE beer_entry_id IN (@entry_31821,@entry_31822,@entry_31823,@entry_31824,@entry_31825,@entry_32301,@entry_32302,@entry_32303,@entry_32304,@entry_32305,@entry_11861,@entry_11862,@entry_11863);
DELETE FROM entry_scan_label WHERE beer_entry_id IN (@entry_31821,@entry_31822,@entry_31823,@entry_31824,@entry_31825,@entry_32301,@entry_32302,@entry_32303,@entry_32304,@entry_32305,@entry_11861,@entry_11862,@entry_11863);
DELETE FROM beer_entry WHERE id IN (@entry_31821,@entry_31822,@entry_31823,@entry_31824,@entry_31825,@entry_32301,@entry_32302,@entry_32303,@entry_32304,@entry_32305,@entry_11861,@entry_11862,@entry_11863);

DELETE account
FROM portal_account account
JOIN brewery brewery ON brewery.id = account.brewery_id
LEFT JOIN beer_entry entry ON entry.brewery_id = brewery.id
WHERE account.phone = '13917195955'
  AND brewery.company_name = CONVERT(0x4441444f444f USING utf8mb4)
  AND entry.id IS NULL;

DELETE brewery
FROM brewery brewery
LEFT JOIN beer_entry entry ON entry.brewery_id = brewery.id
LEFT JOIN portal_account account ON account.brewery_id = brewery.id
WHERE brewery.phone = '13917195955'
  AND brewery.company_name = CONVERT(0x4441444f444f USING utf8mb4)
  AND entry.id IS NULL AND account.id IS NULL;

DELETE style
FROM competition_style_config style
LEFT JOIN beer_entry entry ON entry.style_config_id = style.id
WHERE style.competition_id = 1070
  AND style.source_library_version = 'REGISTRATION_FORM_20260805'
  AND style.style_code LIKE 'IMPORT-1070-ADD-20260805-%'
  AND entry.id IS NULL;

COMMIT;

SELECT 'competition_entries_after_rollback' check_item, COUNT(*) value
FROM beer_entry WHERE competition_id = 1070 AND deleted_flag = 0;
SELECT 'targets_after_rollback' check_item, COUNT(*) value
FROM entry_scan_label WHERE competition_id = 1070 AND short_code IN ('31821','31822','31823','31824','31825','32301','32302','32303','32304','32305','11861','11862','11863');

