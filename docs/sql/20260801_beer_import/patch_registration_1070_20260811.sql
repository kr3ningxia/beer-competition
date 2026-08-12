-- Competition 1070: import seven entries submitted on 2026-08-08 and 2026-08-11.
SET NAMES utf8mb4;
SET time_zone = '+08:00';
START TRANSACTION;

CREATE TEMPORARY TABLE tmp_import_1070_20260811_brewery (
  brewery_key VARCHAR(32) PRIMARY KEY, company_name VARCHAR(255) NOT NULL,
  contact_name VARCHAR(255), phone VARCHAR(32) NOT NULL, wechat VARCHAR(255), brewery_id BIGINT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
INSERT INTO tmp_import_1070_20260811_brewery (brewery_key,company_name,contact_name,phone,wechat) VALUES
('huaya',CONVERT(0xe88ab1e99b85e985bfe980a0 USING utf8mb4),CONVERT(0xe4bb98e5b08fe68792 USING utf8mb4),'15954545181',CONVERT(0x66756c696a696a75 USING utf8mb4)),
('beiwu',CONVERT(0xe69dafe590bee985bfe980a0 USING utf8mb4),CONVERT(0xe9a9ace8b783 USING utf8mb4),'18208851300',CONVERT(0x3138323038383531333030 USING utf8mb4)),
('douniang',CONVERT(0xe69697e985bf USING utf8mb4),CONVERT(0xe69697e985bf USING utf8mb4),'13706205156',CONVERT(0x3133373036323035313536 USING utf8mb4));
UPDATE tmp_import_1070_20260811_brewery target
LEFT JOIN portal_account account ON account.phone=target.phone
LEFT JOIN brewery phone_brewery ON phone_brewery.phone=target.phone
LEFT JOIN brewery name_brewery ON name_brewery.company_name=target.company_name
SET target.brewery_id=COALESCE(account.brewery_id,phone_brewery.id,name_brewery.id);
INSERT INTO brewery (company_name,contact_name,phone,wechat)
SELECT company_name,contact_name,phone,wechat FROM tmp_import_1070_20260811_brewery WHERE brewery_id IS NULL;
UPDATE tmp_import_1070_20260811_brewery target JOIN brewery brewery ON brewery.phone=target.phone
SET target.brewery_id=brewery.id WHERE target.brewery_id IS NULL;
UPDATE brewery brewery JOIN tmp_import_1070_20260811_brewery target ON target.brewery_id=brewery.id
SET brewery.company_name=target.company_name,brewery.contact_name=target.contact_name,brewery.phone=target.phone,brewery.wechat=target.wechat;
INSERT INTO portal_account (phone,wechat,display_name,brewery_id,status)
SELECT phone,wechat,company_name,brewery_id,1 FROM tmp_import_1070_20260811_brewery target
WHERE NOT EXISTS (SELECT 1 FROM portal_account account WHERE account.phone=target.phone);
UPDATE portal_account account JOIN tmp_import_1070_20260811_brewery target ON target.phone=account.phone
SET account.brewery_id=target.brewery_id,account.display_name=target.company_name,account.wechat=target.wechat,account.status=1;

CREATE TEMPORARY TABLE tmp_import_1070_20260811_style (
  style_name VARCHAR(255) PRIMARY KEY,import_style_code VARCHAR(64),import_sort_order INT,style_id BIGINT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
INSERT INTO tmp_import_1070_20260811_style (style_name,import_style_code,import_sort_order) VALUES
(CONVERT(0xe59bbde99985e6b7a1e889b2e68b89e6a0bc USING utf8mb4),NULL,NULL),
(CONVERT(0xe696b0e8a5bfe585b0e79aaee5b094e6a3ae USING utf8mb4),NULL,NULL),
(CONVERT(0xe68595e5b0bce9bb91e6b7b1e889b2e595a4e98592 USING utf8mb4),'IMPORT-1070-ADD-20260811-01',1022),
(CONVERT(0xe8a5bfe6b5b7e5b2b8e79aaee5b094e6a3ae USING utf8mb4),NULL,NULL),
(CONVERT(0xe6b3a2e7bd97e79a84e6b5b7e6b3a2e789b9 USING utf8mb4),'IMPORT-1070-ADD-20260811-02',1023);
UPDATE tmp_import_1070_20260811_style target SET target.style_id=(
  SELECT style.id FROM competition_style_config style WHERE style.competition_id=1070 AND style.name=target.style_name
  ORDER BY style.active_flag DESC,style.id DESC LIMIT 1
);
INSERT INTO competition_style_config
  (competition_id,name,category_name,style_code,description,sort_order,active_flag,source_library_version)
SELECT 1070,target.style_name,CONVERT(0xe68aa5e5908de8a1a8e887aae5a1abe59fbae7a180e9a38ee6a0bc USING utf8mb4),target.import_style_code,
       CONVERT(0xe69da5e6ba90efbc9ae9a39ee4b9a6e68aa5e5908de8a1a8e887aae5a1abe59fbae7a180e9a38ee6a0bcefbc8c323032362d30382d30382f313120e696b0e5a29e USING utf8mb4),target.import_sort_order,1,'REGISTRATION_FORM_20260808_20260811'
FROM tmp_import_1070_20260811_style target WHERE target.style_id IS NULL AND target.import_style_code IS NOT NULL;
UPDATE tmp_import_1070_20260811_style target SET target.style_id=(
  SELECT style.id FROM competition_style_config style WHERE style.competition_id=1070 AND style.name=target.style_name
  ORDER BY style.active_flag DESC,style.id DESC LIMIT 1
);

CREATE TEMPORARY TABLE tmp_import_1070_20260811_entry (
  uuid CHAR(36) PRIMARY KEY,brewery_key VARCHAR(32) NOT NULL,category_id BIGINT NOT NULL,
  entry_name VARCHAR(255) NOT NULL,style_name VARCHAR(255) NOT NULL,abv DECIMAL(5,2) NOT NULL,
  ingredients TEXT NOT NULL,promotion VARCHAR(16) NOT NULL,extra_fields_json TEXT NOT NULL,
  full_code VARCHAR(32) NOT NULL,short_code VARCHAR(5) NOT NULL UNIQUE,label_code VARCHAR(64) NOT NULL,
  scan_token VARCHAR(128) NOT NULL,brewery_id BIGINT NULL,style_id BIGINT NULL,entry_id BIGINT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
INSERT INTO tmp_import_1070_20260811_entry
  (uuid,brewery_key,category_id,entry_name,style_name,abv,ingredients,promotion,extra_fields_json,full_code,short_code,label_code,scan_token) VALUES
('af001b8a-b426-4a5d-b49b-868355fd4134','huaya',1593,CONVERT(0xe697a5e5bc8fe6b395e6a38d USING utf8mb4),CONVERT(0xe59bbde99985e6b7a1e889b2e68b89e6a0bc USING utf8mb4),5.92,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'5368094154531','54531','BE-IMP1070-ADD-AUG08-11-001','Q45093FADC48A6EE0F10E43B322D1C7AD76B3766BE54E1E93260806356A56053'),
('32ea5806-6ef3-40db-a926-dedaa72e16cf','huaya',1593,CONVERT(0xe6a392e6b395e6a38d USING utf8mb4),CONVERT(0xe59bbde99985e6b7a1e889b2e68b89e6a0bc USING utf8mb4),5.59,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'5368094154532','54532','BE-IMP1070-ADD-AUG08-11-002','Q2EB40246E909C43524D717CBFBC683BE2033563F02D969C5582073D4E64E5CB'),
('e91a526c-4e54-4bd8-b578-f4a281f172e4','huaya',1593,CONVERT(0xe696b0e6b395e6a38d USING utf8mb4),CONVERT(0xe696b0e8a5bfe585b0e79aaee5b094e6a3ae USING utf8mb4),5.39,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'5368094154533','54533','BE-IMP1070-ADD-AUG08-11-003','QD61E565A3723ABF34A29AA2DAB403F343AC2D27A88F4C9B98EA33B9830B0BCC'),
('46d272b3-e57f-45ec-b00b-f436fd22e9ee','huaya',1594,CONVERT(0xe9bb91e6b395e6a38d USING utf8mb4),CONVERT(0xe68595e5b0bce9bb91e6b7b1e889b2e595a4e98592 USING utf8mb4),5.08,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'5368094154534','54534','BE-IMP1070-ADD-AUG08-11-004','Q28B2C50982B43D253B436D7A6F9522CE41FF414BE7203914B26106B4F0C003C'),
('872bcded-b72d-41bf-b6a2-341dde50c25c','beiwu',1593,CONVERT(0xe7a9bae6be882de697a5e5bc8fe5a4a7e7b1b3e68b89e6a0bc USING utf8mb4),CONVERT(0xe59bbde99985e6b7a1e889b2e68b89e6a0bc USING utf8mb4),5.50,CONVERT(0xe4bdbfe794a87733342f3730e68b89e6a0bce985b5e6af8defbc8ce58e9fe69699e4b8ade58aa0e585a5323025e5a4a7e7b1b3e78987 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e4bdbfe794a87733342f3730e68b89e6a0bce985b5e6af8defbc8ce58e9fe69699e4b8ade58aa0e585a5323025e5a4a7e7b1b3e78987222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'894843462884','62884','BE-IMP1070-ADD-AUG08-11-005','Q30508C2CF7694A2351FBA62FA1F4DD7778B88439540FC2D6193F666DE4F2680'),
('9f72fb5e-b34a-4ae7-810d-cce39dd3a029','beiwu',1593,CONVERT(0xe7a481e79fb3e4b98be4b88a2de8a5bfe6b5b7e5b2b8e79aaee5b094e6a3ae USING utf8mb4),CONVERT(0xe8a5bfe6b5b7e5b2b8e79aaee5b094e6a3ae USING utf8mb4),5.50,CONVERT(0xe4bdbfe794a86e6f7661e68b89e6a0bce985b5e6af8de58f91e985b5efbc8ce5908ee69c9fe5b9b2e68a95e7be8ee5bc8fe98592e88ab1efbc8ce5b8a6e69da5e5b9b2e88486e6b885e788bde98592e4bd93e5928ce783ade5b8a6e6b0b4e69e9ce9a699e6b094e38082 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e4bdbfe794a86e6f7661e68b89e6a0bce985b5e6af8de58f91e985b5efbc8ce5908ee69c9fe5b9b2e68a95e7be8ee5bc8fe98592e88ab1efbc8ce5b8a6e69da5e5b9b2e88486e6b885e788bde98592e4bd93e5928ce783ade5b8a6e6b0b4e69e9ce9a699e6b094e38082222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'867688255190','55190','BE-IMP1070-ADD-AUG08-11-006','Q006756F8DC281C5F55E3195066B41804A062936DD7DD36F81CD11F736DED62F'),
('cb63c771-0342-4e57-b4dc-ed6a2a792882','douniang',1594,CONVERT(0xe9ad85e5bdb1 USING utf8mb4),CONVERT(0xe6b3a2e7bd97e79a84e6b5b7e6b3a2e789b9 USING utf8mb4),6.50,CONVERT(0xe89c82e89c9c USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e89c82e89c9c222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'330706981451','81451','BE-IMP1070-ADD-AUG08-11-007','Q24A3FC189C4E0FE198556DA0E99F9CE6410B5E4E003B644B3A6EDC7B4C169A6');
UPDATE tmp_import_1070_20260811_entry entry
JOIN tmp_import_1070_20260811_brewery brewery ON brewery.brewery_key=entry.brewery_key
JOIN tmp_import_1070_20260811_style style ON style.style_name=entry.style_name
SET entry.brewery_id=brewery.brewery_id,entry.style_id=style.style_id;

CREATE TEMPORARY TABLE tmp_import_1070_20260811_assert (value INT NOT NULL CHECK(value=0));
INSERT INTO tmp_import_1070_20260811_assert SELECT COUNT(*) FROM tmp_import_1070_20260811_entry WHERE brewery_id IS NULL OR style_id IS NULL;
INSERT INTO tmp_import_1070_20260811_assert
SELECT COUNT(*) FROM entry_scan_label label JOIN beer_entry entry ON entry.id=label.beer_entry_id
WHERE label.competition_id=1070 AND label.short_code IN ('54531','54532','54533','54534','62884','55190','81451') AND entry.uuid NOT IN ('af001b8a-b426-4a5d-b49b-868355fd4134','32ea5806-6ef3-40db-a926-dedaa72e16cf','e91a526c-4e54-4bd8-b578-f4a281f172e4','46d272b3-e57f-45ec-b00b-f436fd22e9ee','872bcded-b72d-41bf-b6a2-341dde50c25c','9f72fb5e-b34a-4ae7-810d-cce39dd3a029','cb63c771-0342-4e57-b4dc-ed6a2a792882');

INSERT INTO beer_entry
  (uuid,competition_id,brewery_id,registration_batch_id,category_id,name,style,style_config_id,abv,extra_fields_json,status,stored_flag,deleted_flag)
SELECT target.uuid,1070,target.brewery_id,NULL,target.category_id,target.entry_name,target.style_name,target.style_id,target.abv,
       target.extra_fields_json,'REGISTERED',0,0 FROM tmp_import_1070_20260811_entry target
WHERE NOT EXISTS (SELECT 1 FROM beer_entry existing WHERE existing.competition_id=1070 AND existing.uuid=target.uuid AND existing.deleted_flag=0);
UPDATE tmp_import_1070_20260811_entry target JOIN beer_entry entry
  ON entry.competition_id=1070 AND entry.uuid=target.uuid AND entry.deleted_flag=0 SET target.entry_id=entry.id;
UPDATE beer_entry entry JOIN tmp_import_1070_20260811_entry target ON target.entry_id=entry.id
SET entry.brewery_id=target.brewery_id,entry.category_id=target.category_id,entry.name=target.entry_name,entry.style=target.style_name,
    entry.style_config_id=target.style_id,entry.abv=target.abv,entry.extra_fields_json=target.extra_fields_json,
    entry.status='REGISTERED',entry.stored_flag=0,entry.deleted_flag=0;
INSERT INTO entry_scan_label (competition_id,beer_entry_id,label_code,short_code,scan_token,status,generated_by,generated_time)
SELECT 1070,target.entry_id,target.label_code,target.short_code,target.scan_token,'ACTIVE',NULL,NOW()
FROM tmp_import_1070_20260811_entry target
WHERE NOT EXISTS (SELECT 1 FROM entry_scan_label label WHERE label.beer_entry_id=target.entry_id AND label.status='ACTIVE');
INSERT INTO beer_entry_extra_field (beer_entry_id,field_key,field_label,field_value)
SELECT target.entry_id,config.field_key,config.field_label,target.ingredients
FROM tmp_import_1070_20260811_entry target JOIN entry_field_config config
  ON config.competition_id=1070 AND config.field_key='specialIngredients' AND config.active_flag=1
WHERE NOT EXISTS (SELECT 1 FROM beer_entry_extra_field existing WHERE existing.beer_entry_id=target.entry_id AND existing.field_key=config.field_key);
INSERT INTO beer_entry_extra_field (beer_entry_id,field_key,field_label,field_value)
SELECT target.entry_id,config.field_key,config.field_label,target.promotion
FROM tmp_import_1070_20260811_entry target JOIN entry_field_config config
  ON config.competition_id=1070 AND config.field_key='custom_1785549677921_0089y' AND config.active_flag=1
WHERE NOT EXISTS (SELECT 1 FROM beer_entry_extra_field existing WHERE existing.beer_entry_id=target.entry_id AND existing.field_key=config.field_key);
UPDATE beer_entry_extra_field extra JOIN tmp_import_1070_20260811_entry target ON target.entry_id=extra.beer_entry_id
SET extra.field_value=CASE extra.field_key WHEN 'specialIngredients' THEN target.ingredients
  WHEN 'custom_1785549677921_0089y' THEN target.promotion ELSE extra.field_value END
WHERE extra.field_key IN ('specialIngredients','custom_1785549677921_0089y');
COMMIT;

SELECT label.short_code,entry.name,brewery.company_name,entry.style,entry.abv,label.status
FROM beer_entry entry JOIN brewery brewery ON brewery.id=entry.brewery_id JOIN entry_scan_label label ON label.beer_entry_id=entry.id
WHERE entry.competition_id=1070 AND entry.deleted_flag=0 AND label.short_code IN ('54531','54532','54533','54534','62884','55190','81451') ORDER BY label.short_code;
