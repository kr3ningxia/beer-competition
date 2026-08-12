-- Competition 1070: import four entries submitted on 2026-08-07.
SET NAMES utf8mb4;
SET time_zone = '+08:00';
START TRANSACTION;

SET @brewery_id = (
  SELECT MIN(b.id) FROM brewery b
  LEFT JOIN portal_account a ON a.brewery_id=b.id
  WHERE a.phone='13730613214' OR b.phone='13730613214' OR b.company_name=CONVERT(0xe98080e7958ce595a4e98592 USING utf8mb4)
);
INSERT INTO brewery (company_name,contact_name,phone,wechat)
SELECT CONVERT(0xe98080e7958ce595a4e98592 USING utf8mb4),CONVERT(0xe5a4a7e6b2b3 USING utf8mb4),'13730613214',CONVERT(0x3133373330363133323134 USING utf8mb4)
WHERE @brewery_id IS NULL;
SET @brewery_id = (SELECT MIN(id) FROM brewery WHERE phone='13730613214' OR company_name=CONVERT(0xe98080e7958ce595a4e98592 USING utf8mb4));
UPDATE brewery SET company_name=CONVERT(0xe98080e7958ce595a4e98592 USING utf8mb4),contact_name=CONVERT(0xe5a4a7e6b2b3 USING utf8mb4),
  phone='13730613214',wechat=CONVERT(0x3133373330363133323134 USING utf8mb4) WHERE id=@brewery_id;
INSERT INTO portal_account (phone,wechat,display_name,brewery_id,status)
SELECT '13730613214',CONVERT(0x3133373330363133323134 USING utf8mb4),CONVERT(0xe98080e7958ce595a4e98592 USING utf8mb4),@brewery_id,1
WHERE NOT EXISTS (SELECT 1 FROM portal_account WHERE phone='13730613214');
UPDATE portal_account SET brewery_id=@brewery_id,display_name=CONVERT(0xe98080e7958ce595a4e98592 USING utf8mb4),
  wechat=CONVERT(0x3133373330363133323134 USING utf8mb4),status=1 WHERE phone='13730613214';

SET @style_italian = (SELECT id FROM competition_style_config WHERE competition_id=1070 AND name=CONVERT(0xe6848fe5a4a7e588a9e79aaee5b094e6a3ae USING utf8mb4) ORDER BY active_flag DESC,id DESC LIMIT 1);
SET @style_cold_ipa = (SELECT id FROM competition_style_config WHERE competition_id=1070 AND name=CONVERT(0xe586b7495041 USING utf8mb4) ORDER BY active_flag DESC,id DESC LIMIT 1);
INSERT INTO competition_style_config
  (competition_id,name,category_name,style_code,description,sort_order,active_flag,source_library_version)
SELECT 1070,CONVERT(0xe586b7495041 USING utf8mb4),CONVERT(0xe68aa5e5908de8a1a8e887aae5a1abe59fbae7a180e9a38ee6a0bc USING utf8mb4),'IMPORT-1070-ADD-20260807-01',
       CONVERT(0xe69da5e6ba90efbc9ae9a696e5b18ae4b8ade59bbde68b89e6a0bce5a4a7e8b59be68aa5e5908de8a1a820323032362d30382d303720e696b0e5a29e USING utf8mb4),1021,1,'REGISTRATION_FORM_20260807'
WHERE @style_cold_ipa IS NULL;
SET @style_cold_ipa = (SELECT id FROM competition_style_config WHERE competition_id=1070 AND name=CONVERT(0xe586b7495041 USING utf8mb4) ORDER BY active_flag DESC,id DESC LIMIT 1);
SET @style_mexican = (SELECT id FROM competition_style_config WHERE competition_id=1070 AND name=CONVERT(0xe5a2a8e8a5bfe593a5e68b89e6a0bc USING utf8mb4) ORDER BY active_flag DESC,id DESC LIMIT 1);
SET @style_hoppy = (SELECT id FROM competition_style_config WHERE competition_id=1070 AND name=CONVERT(0xe98592e88ab1e68b89e6a0bc USING utf8mb4) ORDER BY active_flag DESC,id DESC LIMIT 1);

CREATE TEMPORARY TABLE tmp_import_1070_20260807_entry (
  uuid CHAR(36) PRIMARY KEY,category_id BIGINT NOT NULL,entry_name VARCHAR(255) NOT NULL,
  style_name VARCHAR(255) NOT NULL,abv DECIMAL(5,2) NOT NULL,ingredients TEXT NOT NULL,
  promotion VARCHAR(16) NOT NULL,extra_fields_json TEXT NOT NULL,full_code VARCHAR(32) NOT NULL,
  short_code VARCHAR(5) NOT NULL UNIQUE,label_code VARCHAR(64) NOT NULL,scan_token VARCHAR(128) NOT NULL,
  style_id BIGINT NULL,entry_id BIGINT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
INSERT INTO tmp_import_1070_20260807_entry
  (uuid,category_id,entry_name,style_name,abv,ingredients,promotion,extra_fields_json,full_code,short_code,label_code,scan_token) VALUES
('a181e561-f2dd-4f9c-82d2-66e48b35d44f',1593,CONVERT(0xe788b8e788b8 USING utf8mb4),CONVERT(0xe6848fe5a4a7e588a9e79aaee5b094e6a3ae USING utf8mb4),4.80,CONVERT(0x5733342f3730 USING utf8mb4),CONVERT(0xe590a6 USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a225733342f3730222c22637573746f6d5f313738353534393637373932315f3030383979223a22e590a6227d USING utf8mb4),'4822230493641','93641','BE-IMP1070-ADD-AUG07-001','QD1D0634F977FE3CD64551286339A94BE98F584155B1DC4AD272FD8E07C95F60'),
('4f2ce5f1-102c-45ce-9922-7579ea6eca3d',1593,CONVERT(0xe4b880e8b7afe59091e8a5bf USING utf8mb4),CONVERT(0xe586b7495041 USING utf8mb4),7.00,CONVERT(0x7733342f3730 USING utf8mb4),CONVERT(0xe590a6 USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a227733342f3730222c22637573746f6d5f313738353534393637373932315f3030383979223a22e590a6227d USING utf8mb4),'4822230493642','93642','BE-IMP1070-ADD-AUG07-002','QF45974062CA044E463180AAD2EC940181E3C70CA11329C8BA632D1B823BE59C'),
('b3424882-ef67-4d7c-abe2-1dac281c3e09',1593,CONVERT(0xe69993e5a4ab USING utf8mb4),CONVERT(0xe5a2a8e8a5bfe593a5e68b89e6a0bc USING utf8mb4),4.30,CONVERT(0xe4bdbfe794a8e4ba86e78e89e7b1b3e78987efbc8ce4b894e5b9b2e68a95e4ba86e5beaee9878fe88eabe59bbee4be9de58da1e595a4e98592e88ab1 USING utf8mb4),CONVERT(0xe590a6 USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e4bdbfe794a8e4ba86e78e89e7b1b3e78987efbc8ce4b894e5b9b2e68a95e4ba86e5beaee9878fe88eabe59bbee4be9de58da1e595a4e98592e88ab1222c22637573746f6d5f313738353534393637373932315f3030383979223a22e590a6227d USING utf8mb4),'4822230493643','93643','BE-IMP1070-ADD-AUG07-003','Q011118FA7ACB064C596C8DEC54262D89B3EF8863DA3E1B6A474C63A3A0A0853'),
('1d6abf78-9ffc-45f6-9178-a4b5ed48fb73',1595,CONVERT(0xe69f91e6a998e4b990e59bad USING utf8mb4),CONVERT(0xe98592e88ab1e68b89e6a0bc USING utf8mb4),5.70,CONVERT(0x53482d3435efbc8ce98089e794a832e5a49ae7a78de784a6e7b396e9baa6e88abde4b88ee58f8de5bc8fe7b396e68993e980a0e5b182e6aca1e69bb4e5a48de69d82e98592e4bd93 USING utf8mb4),CONVERT(0xe590a6 USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a2253482d3435efbc8ce98089e794a832e5a49ae7a78de784a6e7b396e9baa6e88abde4b88ee58f8de5bc8fe7b396e68993e980a0e5b182e6aca1e69bb4e5a48de69d82e98592e4bd93222c22637573746f6d5f313738353534393637373932315f3030383979223a22e590a6227d USING utf8mb4),'4822230493644','93644','BE-IMP1070-ADD-AUG07-004','Q27F670C185B5847A6B7D2FA2AB3AAC5EBFE36F4F1E621B481F90EAD4F060B94');
UPDATE tmp_import_1070_20260807_entry SET style_id=CASE style_name
  WHEN CONVERT(0xe6848fe5a4a7e588a9e79aaee5b094e6a3ae USING utf8mb4) THEN @style_italian WHEN CONVERT(0xe586b7495041 USING utf8mb4) THEN @style_cold_ipa
  WHEN CONVERT(0xe5a2a8e8a5bfe593a5e68b89e6a0bc USING utf8mb4) THEN @style_mexican WHEN CONVERT(0xe98592e88ab1e68b89e6a0bc USING utf8mb4) THEN @style_hoppy END;

CREATE TEMPORARY TABLE tmp_import_1070_20260807_assert (value INT NOT NULL CHECK(value=0));
INSERT INTO tmp_import_1070_20260807_assert SELECT COUNT(*) FROM tmp_import_1070_20260807_entry WHERE style_id IS NULL;
INSERT INTO tmp_import_1070_20260807_assert
SELECT COUNT(*) FROM entry_scan_label label JOIN beer_entry entry ON entry.id=label.beer_entry_id
WHERE label.competition_id=1070 AND label.short_code IN ('93641','93642','93643','93644') AND entry.uuid NOT IN ('a181e561-f2dd-4f9c-82d2-66e48b35d44f','4f2ce5f1-102c-45ce-9922-7579ea6eca3d','b3424882-ef67-4d7c-abe2-1dac281c3e09','1d6abf78-9ffc-45f6-9178-a4b5ed48fb73');

INSERT INTO beer_entry
  (uuid,competition_id,brewery_id,registration_batch_id,category_id,name,style,style_config_id,abv,extra_fields_json,status,stored_flag,deleted_flag)
SELECT target.uuid,1070,@brewery_id,NULL,target.category_id,target.entry_name,target.style_name,target.style_id,target.abv,target.extra_fields_json,'REGISTERED',0,0
FROM tmp_import_1070_20260807_entry target
WHERE NOT EXISTS (SELECT 1 FROM beer_entry existing WHERE existing.competition_id=1070 AND existing.uuid=target.uuid AND existing.deleted_flag=0);
UPDATE tmp_import_1070_20260807_entry target JOIN beer_entry entry ON entry.competition_id=1070 AND entry.uuid=target.uuid AND entry.deleted_flag=0
SET target.entry_id=entry.id;
UPDATE beer_entry entry JOIN tmp_import_1070_20260807_entry target ON target.entry_id=entry.id
SET entry.brewery_id=@brewery_id,entry.category_id=target.category_id,entry.name=target.entry_name,entry.style=target.style_name,
    entry.style_config_id=target.style_id,entry.abv=target.abv,entry.extra_fields_json=target.extra_fields_json,
    entry.status='REGISTERED',entry.stored_flag=0,entry.deleted_flag=0;

INSERT INTO entry_scan_label
  (competition_id,beer_entry_id,label_code,short_code,scan_token,status,generated_by,generated_time)
SELECT 1070,target.entry_id,target.label_code,target.short_code,target.scan_token,'ACTIVE',NULL,NOW()
FROM tmp_import_1070_20260807_entry target
WHERE NOT EXISTS (SELECT 1 FROM entry_scan_label label WHERE label.beer_entry_id=target.entry_id AND label.status='ACTIVE');

INSERT INTO beer_entry_extra_field (beer_entry_id,field_key,field_label,field_value)
SELECT target.entry_id,config.field_key,config.field_label,target.ingredients
FROM tmp_import_1070_20260807_entry target JOIN entry_field_config config ON config.competition_id=1070 AND config.field_key='specialIngredients' AND config.active_flag=1
WHERE NOT EXISTS (SELECT 1 FROM beer_entry_extra_field existing WHERE existing.beer_entry_id=target.entry_id AND existing.field_key=config.field_key);
INSERT INTO beer_entry_extra_field (beer_entry_id,field_key,field_label,field_value)
SELECT target.entry_id,config.field_key,config.field_label,target.promotion
FROM tmp_import_1070_20260807_entry target JOIN entry_field_config config ON config.competition_id=1070 AND config.field_key='custom_1785549677921_0089y' AND config.active_flag=1
WHERE NOT EXISTS (SELECT 1 FROM beer_entry_extra_field existing WHERE existing.beer_entry_id=target.entry_id AND existing.field_key=config.field_key);
UPDATE beer_entry_extra_field extra JOIN tmp_import_1070_20260807_entry target ON target.entry_id=extra.beer_entry_id
SET extra.field_value=CASE extra.field_key WHEN 'specialIngredients' THEN target.ingredients WHEN 'custom_1785549677921_0089y' THEN target.promotion ELSE extra.field_value END
WHERE extra.field_key IN ('specialIngredients','custom_1785549677921_0089y');
COMMIT;

SELECT label.short_code,entry.name,brewery.company_name,entry.style,entry.abv,label.status
FROM beer_entry entry JOIN brewery brewery ON brewery.id=entry.brewery_id JOIN entry_scan_label label ON label.beer_entry_id=entry.id
WHERE entry.competition_id=1070 AND entry.deleted_flag=0 AND label.short_code IN ('93641','93642','93643','93644') ORDER BY label.short_code;

