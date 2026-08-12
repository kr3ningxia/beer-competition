-- Roll back only the entries and records introduced by the 2026-08-03 patch.
SET NAMES utf8mb4;
SET time_zone = '+08:00';
START TRANSACTION;

SET @entry_85079 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '00cd8a25-1c49-48d1-95f4-6290481685d0'
  LIMIT 1
);
SET @entry_55986 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = 'f6ce02aa-109c-4b09-8787-c0658f31f30a'
  LIMIT 1
);
SET @entry_76015 = (
  SELECT id FROM beer_entry
  WHERE competition_id = 1070 AND uuid = '134e7be3-b740-4912-9cc8-cec836dc1e26'
  LIMIT 1
);

DELETE FROM beer_entry_extra_field
WHERE beer_entry_id IN (@entry_85079, @entry_55986, @entry_76015);
DELETE FROM entry_scan_label
WHERE beer_entry_id IN (@entry_85079, @entry_55986, @entry_76015);
DELETE FROM beer_entry
WHERE id IN (@entry_85079, @entry_55986, @entry_76015);

DELETE account
FROM portal_account account
JOIN brewery brewery ON brewery.id = account.brewery_id
LEFT JOIN beer_entry entry ON entry.brewery_id = brewery.id
WHERE account.phone IN ('15622203218','13351186381')
  AND brewery.company_name IN (
    CONVERT(0x4e4f20574859 USING utf8mb4),
    CONVERT(0xe79c87e4bfae USING utf8mb4)
  )
  AND entry.id IS NULL;

DELETE brewery
FROM brewery brewery
LEFT JOIN beer_entry entry ON entry.brewery_id = brewery.id
LEFT JOIN portal_account account ON account.brewery_id = brewery.id
WHERE brewery.company_name IN (
    CONVERT(0x4e4f20574859 USING utf8mb4),
    CONVERT(0xe79c87e4bfae USING utf8mb4)
  )
  AND entry.id IS NULL
  AND account.id IS NULL;

DELETE style
FROM competition_style_config style
LEFT JOIN beer_entry entry ON entry.style_config_id = style.id
WHERE style.competition_id = 1070
  AND style.source_library_version = 'REGISTRATION_FORM_20260803'
  AND style.style_code = 'IMPORT-1070-ADD-20260803-01'
  AND entry.id IS NULL;

COMMIT;

SELECT 'competition_entries_after_rollback' check_item, COUNT(*) value
FROM beer_entry
WHERE competition_id = 1070 AND deleted_flag = 0;
SELECT 'targets_after_rollback' check_item, COUNT(*) value
FROM entry_scan_label
WHERE competition_id = 1070 AND short_code IN ('85079','55986','76015');
