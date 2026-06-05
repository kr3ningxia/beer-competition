-- Read-only check for damaged Chinese text in local test data.
-- Run with:
-- mysql --default-character-set=utf8mb4 -uroot -p beer_competition --execute="source docs/check-chinese-data-health.sql"

SELECT 'competition' AS table_name, 'name' AS column_name, id AS row_id, name AS text_value, HEX(name) AS text_hex
FROM competition
WHERE HEX(name) LIKE '%3F3F3F%'

UNION ALL
SELECT 'competition_category', 'name', id, name, HEX(name)
FROM competition_category
WHERE HEX(name) LIKE '%3F3F3F%'

UNION ALL
SELECT 'competition_style_config', 'category_name', id, category_name, HEX(category_name)
FROM competition_style_config
WHERE HEX(category_name) LIKE '%3F3F3F%'

UNION ALL
SELECT 'competition_style_config', 'description', id, description, HEX(description)
FROM competition_style_config
WHERE description IS NOT NULL AND HEX(description) LIKE '%3F3F3F%'

UNION ALL
SELECT 'beer_entry', 'name', id, name, HEX(name)
FROM beer_entry
WHERE name IS NOT NULL AND HEX(name) LIKE '%3F3F3F%'

UNION ALL
SELECT 'beer_entry', 'description', id, description, HEX(description)
FROM beer_entry
WHERE description IS NOT NULL AND HEX(description) LIKE '%3F3F3F%';
