USE `beer_competition`;

-- BJCP 2021 Beer Style Guidelines official web directory:
-- https://styles.bjcp.org/bjcp-2021-beer.md
-- This migration stores category and subcategory names/codes only. Long style
-- descriptions remain in the BJCP source.

START TRANSACTION;

INSERT INTO `style_library` (`code`, `name`, `version`, `language`, `source`, `status`, `tags_json`)
VALUES ('BJCP_2021_EN', 'BJCP 2021 Beer Style Guidelines', '2021', 'English', 'BJCP', 1, JSON_ARRAY('Official directory', 'English styles', 'Competition-ready'))
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `version` = VALUES(`version`),
  `language` = VALUES(`language`),
  `source` = VALUES(`source`),
  `status` = VALUES(`status`),
  `tags_json` = VALUES(`tags_json`);

SET @bjcp_2021_en_id := (SELECT `id` FROM `style_library` WHERE `code` = 'BJCP_2021_EN');

DELETE FROM `style_item` WHERE `library_id` = @bjcp_2021_en_id;
DELETE FROM `style_category` WHERE `library_id` = @bjcp_2021_en_id;

INSERT INTO `style_category` (`library_id`, `name`, `sort_order`) VALUES
  (@bjcp_2021_en_id, '1. STANDARD AMERICAN BEER', 1),
  (@bjcp_2021_en_id, '2. INTERNATIONAL LAGER', 2),
  (@bjcp_2021_en_id, '3. CZECH LAGER', 3),
  (@bjcp_2021_en_id, '4. PALE MALTY EUROPEAN LAGER', 4),
  (@bjcp_2021_en_id, '5. PALE BITTER EUROPEAN BEER', 5),
  (@bjcp_2021_en_id, '6. AMBER MALTY EUROPEAN LAGER', 6),
  (@bjcp_2021_en_id, '7. AMBER BITTER EUROPEAN BEER', 7),
  (@bjcp_2021_en_id, '8. DARK EUROPEAN LAGER', 8),
  (@bjcp_2021_en_id, '9. STRONG EUROPEAN BEER', 9),
  (@bjcp_2021_en_id, '10. GERMAN WHEAT BEER', 10),
  (@bjcp_2021_en_id, '11. BRITISH BITTER', 11),
  (@bjcp_2021_en_id, '12. PALE COMMONWEALTH BEER', 12),
  (@bjcp_2021_en_id, '13. BROWN BRITISH BEER', 13),
  (@bjcp_2021_en_id, '14. SCOTTISH ALE', 14),
  (@bjcp_2021_en_id, '15. IRISH BEER', 15),
  (@bjcp_2021_en_id, '16. DARK BRITISH BEER', 16),
  (@bjcp_2021_en_id, '17. STRONG BRITISH ALE', 17),
  (@bjcp_2021_en_id, '18. PALE AMERICAN ALE', 18),
  (@bjcp_2021_en_id, '19. AMBER AND BROWN AMERICAN BEER', 19),
  (@bjcp_2021_en_id, '20. AMERICAN PORTER AND STOUT', 20),
  (@bjcp_2021_en_id, '21. IPA', 21),
  (@bjcp_2021_en_id, '22. STRONG AMERICAN ALE', 22),
  (@bjcp_2021_en_id, '23. EUROPEAN SOUR ALE', 23),
  (@bjcp_2021_en_id, '24. BELGIAN ALE', 24),
  (@bjcp_2021_en_id, '25. STRONG BELGIAN ALE', 25),
  (@bjcp_2021_en_id, '26. MONASTIC ALE', 26),
  (@bjcp_2021_en_id, '27. HISTORICAL BEER', 27),
  (@bjcp_2021_en_id, '28. AMERICAN WILD ALE', 28),
  (@bjcp_2021_en_id, '29. FRUIT BEER', 29),
  (@bjcp_2021_en_id, '30. SPICED BEER', 30),
  (@bjcp_2021_en_id, '31. ALTERNATIVE FERMENTABLES BEER', 31),
  (@bjcp_2021_en_id, '32. SMOKED BEER', 32),
  (@bjcp_2021_en_id, '33. WOOD BEER', 33),
  (@bjcp_2021_en_id, '34. SPECIALTY BEER', 34);

INSERT INTO `style_item` (`library_id`, `category_id`, `name`, `style_code`, `description`, `status`, `sort_order`)
SELECT @bjcp_2021_en_id, category.`id`, item.`name`, item.`style_code`, NULL, 1, item.`sort_order`
FROM (
  SELECT '1. STANDARD AMERICAN BEER' AS category_name, 'American Light Lager' AS name, '1A' AS style_code, 1 AS sort_order UNION ALL
  SELECT '1. STANDARD AMERICAN BEER', 'American Lager', '1B', 2 UNION ALL
  SELECT '1. STANDARD AMERICAN BEER', 'Cream Ale', '1C', 3 UNION ALL
  SELECT '1. STANDARD AMERICAN BEER', 'American Wheat Beer', '1D', 4 UNION ALL
  SELECT '2. INTERNATIONAL LAGER', 'International Pale Lager', '2A', 5 UNION ALL
  SELECT '2. INTERNATIONAL LAGER', 'International Amber Lager', '2B', 6 UNION ALL
  SELECT '2. INTERNATIONAL LAGER', 'International Dark Lager', '2C', 7 UNION ALL
  SELECT '3. CZECH LAGER', 'Czech Pale Lager', '3A', 8 UNION ALL
  SELECT '3. CZECH LAGER', 'Czech Premium Pale Lager', '3B', 9 UNION ALL
  SELECT '3. CZECH LAGER', 'Czech Amber Lager', '3C', 10 UNION ALL
  SELECT '3. CZECH LAGER', 'Czech Dark Lager', '3D', 11 UNION ALL
  SELECT '4. PALE MALTY EUROPEAN LAGER', 'Munich Helles', '4A', 12 UNION ALL
  SELECT '4. PALE MALTY EUROPEAN LAGER', 'Festbier', '4B', 13 UNION ALL
  SELECT '4. PALE MALTY EUROPEAN LAGER', 'Helles Bock', '4C', 14 UNION ALL
  SELECT '5. PALE BITTER EUROPEAN BEER', 'German Leichtbier', '5A', 15 UNION ALL
  SELECT '5. PALE BITTER EUROPEAN BEER', 'Kölsch', '5B', 16 UNION ALL
  SELECT '5. PALE BITTER EUROPEAN BEER', 'German Helles Exportbier', '5C', 17 UNION ALL
  SELECT '5. PALE BITTER EUROPEAN BEER', 'German Pils', '5D', 18 UNION ALL
  SELECT '6. AMBER MALTY EUROPEAN LAGER', 'Märzen', '6A', 19 UNION ALL
  SELECT '6. AMBER MALTY EUROPEAN LAGER', 'Rauchbier', '6B', 20 UNION ALL
  SELECT '6. AMBER MALTY EUROPEAN LAGER', 'Dunkles Bock', '6C', 21 UNION ALL
  SELECT '7. AMBER BITTER EUROPEAN BEER', 'Vienna Lager', '7A', 22 UNION ALL
  SELECT '7. AMBER BITTER EUROPEAN BEER', 'Altbier', '7B', 23 UNION ALL
  SELECT '8. DARK EUROPEAN LAGER', 'Munich Dunkel', '8A', 24 UNION ALL
  SELECT '8. DARK EUROPEAN LAGER', 'Schwarzbier', '8B', 25 UNION ALL
  SELECT '9. STRONG EUROPEAN BEER', 'Doppelbock', '9A', 26 UNION ALL
  SELECT '9. STRONG EUROPEAN BEER', 'Eisbock', '9B', 27 UNION ALL
  SELECT '9. STRONG EUROPEAN BEER', 'Baltic Porter', '9C', 28 UNION ALL
  SELECT '10. GERMAN WHEAT BEER', 'Weissbier', '10A', 29 UNION ALL
  SELECT '10. GERMAN WHEAT BEER', 'Dunkles Weissbier', '10B', 30 UNION ALL
  SELECT '10. GERMAN WHEAT BEER', 'Weizenbock', '10C', 31 UNION ALL
  SELECT '11. BRITISH BITTER', 'Ordinary Bitter', '11A', 32 UNION ALL
  SELECT '11. BRITISH BITTER', 'Best Bitter', '11B', 33 UNION ALL
  SELECT '11. BRITISH BITTER', 'Strong Bitter', '11C', 34 UNION ALL
  SELECT '12. PALE COMMONWEALTH BEER', 'British Golden Ale', '12A', 35 UNION ALL
  SELECT '12. PALE COMMONWEALTH BEER', 'Australian Sparkling Ale', '12B', 36 UNION ALL
  SELECT '12. PALE COMMONWEALTH BEER', 'English IPA', '12C', 37 UNION ALL
  SELECT '13. BROWN BRITISH BEER', 'Dark Mild', '13A', 38 UNION ALL
  SELECT '13. BROWN BRITISH BEER', 'British Brown Ale', '13B', 39 UNION ALL
  SELECT '13. BROWN BRITISH BEER', 'English Porter', '13C', 40 UNION ALL
  SELECT '14. SCOTTISH ALE', 'Scottish Light', '14A', 41 UNION ALL
  SELECT '14. SCOTTISH ALE', 'Scottish Heavy', '14B', 42 UNION ALL
  SELECT '14. SCOTTISH ALE', 'Scottish Export', '14C', 43 UNION ALL
  SELECT '15. IRISH BEER', 'Irish Red Ale', '15A', 44 UNION ALL
  SELECT '15. IRISH BEER', 'Irish Stout', '15B', 45 UNION ALL
  SELECT '15. IRISH BEER', 'Irish Extra Stout', '15C', 46 UNION ALL
  SELECT '16. DARK BRITISH BEER', 'Sweet Stout', '16A', 47 UNION ALL
  SELECT '16. DARK BRITISH BEER', 'Oatmeal Stout', '16B', 48 UNION ALL
  SELECT '16. DARK BRITISH BEER', 'Tropical Stout', '16C', 49 UNION ALL
  SELECT '16. DARK BRITISH BEER', 'Foreign Extra Stout', '16D', 50 UNION ALL
  SELECT '17. STRONG BRITISH ALE', 'British Strong Ale', '17A', 51 UNION ALL
  SELECT '17. STRONG BRITISH ALE', 'Old Ale', '17B', 52 UNION ALL
  SELECT '17. STRONG BRITISH ALE', 'Wee Heavy', '17C', 53 UNION ALL
  SELECT '17. STRONG BRITISH ALE', 'English Barleywine', '17D', 54 UNION ALL
  SELECT '18. PALE AMERICAN ALE', 'Blonde Ale', '18A', 55 UNION ALL
  SELECT '18. PALE AMERICAN ALE', 'American Pale Ale', '18B', 56 UNION ALL
  SELECT '19. AMBER AND BROWN AMERICAN BEER', 'American Amber Ale', '19A', 57 UNION ALL
  SELECT '19. AMBER AND BROWN AMERICAN BEER', 'California Common', '19B', 58 UNION ALL
  SELECT '19. AMBER AND BROWN AMERICAN BEER', 'American Brown Ale', '19C', 59 UNION ALL
  SELECT '20. AMERICAN PORTER AND STOUT', 'American Porter', '20A', 60 UNION ALL
  SELECT '20. AMERICAN PORTER AND STOUT', 'American Stout', '20B', 61 UNION ALL
  SELECT '20. AMERICAN PORTER AND STOUT', 'Imperial Stout', '20C', 62 UNION ALL
  SELECT '21. IPA', 'American IPA', '21A', 63 UNION ALL
  SELECT '21. IPA', 'Specialty IPA', '21B', 64 UNION ALL
  SELECT '21. IPA', 'Hazy IPA', '21C', 65 UNION ALL
  SELECT '22. STRONG AMERICAN ALE', 'Double IPA', '22A', 66 UNION ALL
  SELECT '22. STRONG AMERICAN ALE', 'American Strong Ale', '22B', 67 UNION ALL
  SELECT '22. STRONG AMERICAN ALE', 'American Barleywine', '22C', 68 UNION ALL
  SELECT '22. STRONG AMERICAN ALE', 'Wheatwine', '22D', 69 UNION ALL
  SELECT '23. EUROPEAN SOUR ALE', 'Berliner Weisse', '23A', 70 UNION ALL
  SELECT '23. EUROPEAN SOUR ALE', 'Flanders Red Ale', '23B', 71 UNION ALL
  SELECT '23. EUROPEAN SOUR ALE', 'Oud Bruin', '23C', 72 UNION ALL
  SELECT '23. EUROPEAN SOUR ALE', 'Lambic', '23D', 73 UNION ALL
  SELECT '23. EUROPEAN SOUR ALE', 'Gueuze', '23E', 74 UNION ALL
  SELECT '23. EUROPEAN SOUR ALE', 'Fruit Lambic', '23F', 75 UNION ALL
  SELECT '23. EUROPEAN SOUR ALE', 'Gose', '23G', 76 UNION ALL
  SELECT '24. BELGIAN ALE', 'Witbier', '24A', 77 UNION ALL
  SELECT '24. BELGIAN ALE', 'Belgian Pale Ale', '24B', 78 UNION ALL
  SELECT '24. BELGIAN ALE', 'Bière de Garde', '24C', 79 UNION ALL
  SELECT '25. STRONG BELGIAN ALE', 'Belgian Blond Ale', '25A', 80 UNION ALL
  SELECT '25. STRONG BELGIAN ALE', 'Saison', '25B', 81 UNION ALL
  SELECT '25. STRONG BELGIAN ALE', 'Belgian Golden Strong Ale', '25C', 82 UNION ALL
  SELECT '26. MONASTIC ALE', 'Belgian Single', '26A', 83 UNION ALL
  SELECT '26. MONASTIC ALE', 'Belgian Dubbel', '26B', 84 UNION ALL
  SELECT '26. MONASTIC ALE', 'Belgian Tripel', '26C', 85 UNION ALL
  SELECT '26. MONASTIC ALE', 'Belgian Dark Strong Ale', '26D', 86 UNION ALL
  SELECT '27. HISTORICAL BEER', 'Kellerbier', '27A', 87 UNION ALL
  SELECT '27. HISTORICAL BEER', 'Kentucky Common', '27B', 88 UNION ALL
  SELECT '27. HISTORICAL BEER', 'Lichtenhainer', '27C', 89 UNION ALL
  SELECT '27. HISTORICAL BEER', 'London Brown Ale', '27D', 90 UNION ALL
  SELECT '27. HISTORICAL BEER', 'Piwo Grodziskie', '27E', 91 UNION ALL
  SELECT '27. HISTORICAL BEER', 'Pre-Prohibition Lager', '27F', 92 UNION ALL
  SELECT '27. HISTORICAL BEER', 'Pre-Prohibition Porter', '27G', 93 UNION ALL
  SELECT '27. HISTORICAL BEER', 'Roggenbier', '27H', 94 UNION ALL
  SELECT '27. HISTORICAL BEER', 'Sahti', '27I', 95 UNION ALL
  SELECT '28. AMERICAN WILD ALE', 'Brett Beer', '28A', 96 UNION ALL
  SELECT '28. AMERICAN WILD ALE', 'Mixed-Fermentation Sour Beer', '28B', 97 UNION ALL
  SELECT '28. AMERICAN WILD ALE', 'Wild Specialty Beer', '28C', 98 UNION ALL
  SELECT '28. AMERICAN WILD ALE', 'Straight Sour Beer', '28D', 99 UNION ALL
  SELECT '29. FRUIT BEER', 'Fruit Beer', '29A', 100 UNION ALL
  SELECT '29. FRUIT BEER', 'Fruit and Spice Beer', '29B', 101 UNION ALL
  SELECT '29. FRUIT BEER', 'Specialty Fruit Beer', '29C', 102 UNION ALL
  SELECT '29. FRUIT BEER', 'Grape Ale', '29D', 103 UNION ALL
  SELECT '30. SPICED BEER', 'Spice, Herb, or Vegetable Beer', '30A', 104 UNION ALL
  SELECT '30. SPICED BEER', 'Autumn Seasonal Beer', '30B', 105 UNION ALL
  SELECT '30. SPICED BEER', 'Winter Seasonal Beer', '30C', 106 UNION ALL
  SELECT '30. SPICED BEER', 'Specialty Spice Beer', '30D', 107 UNION ALL
  SELECT '31. ALTERNATIVE FERMENTABLES BEER', 'Alternative Grain Beer', '31A', 108 UNION ALL
  SELECT '31. ALTERNATIVE FERMENTABLES BEER', 'Alternative Sugar Beer', '31B', 109 UNION ALL
  SELECT '32. SMOKED BEER', 'Classic Style Smoked Beer', '32A', 110 UNION ALL
  SELECT '32. SMOKED BEER', 'Specialty Smoked Beer', '32B', 111 UNION ALL
  SELECT '33. WOOD BEER', 'Wood-Aged Beer', '33A', 112 UNION ALL
  SELECT '33. WOOD BEER', 'Specialty Wood-Aged Beer', '33B', 113 UNION ALL
  SELECT '34. SPECIALTY BEER', 'Commercial Specialty Beer', '34A', 114 UNION ALL
  SELECT '34. SPECIALTY BEER', 'Mixed-Style Beer', '34B', 115 UNION ALL
  SELECT '34. SPECIALTY BEER', 'Experimental Beer', '34C', 116
) item
JOIN `style_category` category ON category.`library_id` = @bjcp_2021_en_id AND category.`name` = item.`category_name`;

COMMIT;
