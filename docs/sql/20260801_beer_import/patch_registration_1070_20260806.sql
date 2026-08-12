-- Competition 1070: import 14 entries submitted on 2026-08-06.
SET NAMES utf8mb4;
SET time_zone = '+08:00';
START TRANSACTION;

CREATE TEMPORARY TABLE tmp_import_1070_20260806_brewery (
  brewery_key VARCHAR(32) PRIMARY KEY,
  company_name VARCHAR(255) NOT NULL,
  contact_name VARCHAR(255),
  phone VARCHAR(32) NOT NULL,
  wechat VARCHAR(255),
  brewery_id BIGINT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

INSERT INTO tmp_import_1070_20260806_brewery
  (brewery_key, company_name, contact_name, phone, wechat) VALUES
('xinghui',CONVERT(0xe5b9b8e4bc9ae985bfe980a0e595a4e98592e58e82 USING utf8mb4),CONVERT(0xe4beafe4bc9f USING utf8mb4),'18643873000',CONVERT(0x3138363433383733303030 USING utf8mb4)),
('zhizaofu',CONVERT(0xe7bb87e980a0e5ba9c USING utf8mb4),CONVERT(0xe58589e58589 USING utf8mb4),'13584061302',CONVERT(0x4f726967616d6930363237 USING utf8mb4)),
('nanmen',CONVERT(0xe68890e983bde58d97e997a8e7b2bee985bf USING utf8mb4),CONVERT(0xe8aeb8e69687e9ab98 USING utf8mb4),'18802771906',CONVERT(0x78776778776732303035 USING utf8mb4)),
('youhashu',CONVERT(0xe69c89e59388e695b0 USING utf8mb4),CONVERT(0xe998bfe6a8b5 USING utf8mb4),'13341305088',CONVERT(0x3333363737383533 USING utf8mb4));

UPDATE tmp_import_1070_20260806_brewery target
LEFT JOIN portal_account account ON account.phone = target.phone
LEFT JOIN brewery phone_brewery ON phone_brewery.phone = target.phone
LEFT JOIN brewery name_brewery ON name_brewery.company_name = target.company_name
SET target.brewery_id = COALESCE(account.brewery_id, phone_brewery.id, name_brewery.id);

INSERT INTO brewery (company_name, contact_name, phone, wechat)
SELECT company_name, contact_name, phone, wechat
FROM tmp_import_1070_20260806_brewery
WHERE brewery_id IS NULL;

UPDATE tmp_import_1070_20260806_brewery target
JOIN brewery brewery ON brewery.phone = target.phone
SET target.brewery_id = brewery.id
WHERE target.brewery_id IS NULL;

UPDATE brewery brewery
JOIN tmp_import_1070_20260806_brewery target ON target.brewery_id = brewery.id
SET brewery.company_name = target.company_name,
    brewery.contact_name = target.contact_name,
    brewery.phone = target.phone,
    brewery.wechat = target.wechat;

INSERT INTO portal_account (phone, wechat, display_name, brewery_id, status)
SELECT target.phone, target.wechat, target.company_name, target.brewery_id, 1
FROM tmp_import_1070_20260806_brewery target
WHERE NOT EXISTS (SELECT 1 FROM portal_account account WHERE account.phone = target.phone);

UPDATE portal_account account
JOIN tmp_import_1070_20260806_brewery target ON target.phone = account.phone
SET account.brewery_id = target.brewery_id,
    account.display_name = target.company_name,
    account.wechat = target.wechat,
    account.status = 1;

CREATE TEMPORARY TABLE tmp_import_1070_20260806_style (
  style_name VARCHAR(255) PRIMARY KEY,
  import_style_code VARCHAR(64),
  import_sort_order INT,
  style_id BIGINT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

INSERT INTO tmp_import_1070_20260806_style
  (style_name, import_style_code, import_sort_order) VALUES
(CONVERT(0xe5a2a8e8a5bfe593a5e78e89e7b1b3e68b89e6a0bc USING utf8mb4),'IMPORT-1070-ADD-20260806-01',1015),
(CONVERT(0xe4bca0e7bb9fe5beb7e5bc8fe79aaee5b094e6a3ae USING utf8mb4),'IMPORT-1070-ADD-20260806-02',1016),
(CONVERT(0xe5a4a7e7b1b3e68b89e6a0bc USING utf8mb4),NULL,NULL),
(CONVERT(0xe68db7e5858be6b7a1e889b2e68b89e6a0bc USING utf8mb4),NULL,NULL),
(CONVERT(0xe98592e88ab1e68b89e6a0bc USING utf8mb4),NULL,NULL),
(CONVERT(0xe6a48de789a9e5a29ee591b3e5889be6848fe68b89e6a0bc USING utf8mb4),'IMPORT-1070-ADD-20260806-03',1017),
(CONVERT(0xe697a5e5bc8fe5a4a7e7b1b3e68b89e6a0bc USING utf8mb4),'IMPORT-1070-ADD-20260806-04',1018),
(CONVERT(0xe6b3a2e8a5bfe7b1b3e4ba9ae79aaee5b094e6a3ae USING utf8mb4),NULL,NULL),
(CONVERT(0xe4b8ade5bc8fe68b89e6a0bc USING utf8mb4),'IMPORT-1070-ADD-20260806-05',1019),
(CONVERT(0xe5a2a8e8a5bfe593a5e68b89e6a0bc USING utf8mb4),NULL,NULL),
(CONVERT(0xe59bbde99985e6b7a1e889b2e68b89e6a0bc USING utf8mb4),NULL,NULL),
(CONVERT(0xe5beb7e5bc8fe6b885e4baaee68b89e6a0bc USING utf8mb4),NULL,NULL),
(CONVERT(0xe697a9e9a490e79aaee5b094e6a3ae USING utf8mb4),'IMPORT-1070-ADD-20260806-06',1020);

UPDATE tmp_import_1070_20260806_style target
SET target.style_id = (
  SELECT style.id FROM competition_style_config style
  WHERE style.competition_id = 1070
    AND style.name = target.style_name
  ORDER BY style.active_flag DESC, style.id DESC LIMIT 1
);

INSERT INTO competition_style_config
  (competition_id, name, category_name, style_code, description,
   sort_order, active_flag, source_library_version)
SELECT 1070, target.style_name, CONVERT(0xe68aa5e5908de8a1a8e887aae5a1abe59fbae7a180e9a38ee6a0bc USING utf8mb4),
       target.import_style_code, CONVERT(0xe69da5e6ba90efbc9ae9a696e5b18ae4b8ade59bbde68b89e6a0bce5a4a7e8b59be68aa5e5908de8a1a820323032362d30382d303620e696b0e5a29e USING utf8mb4),
       target.import_sort_order, 1, 'REGISTRATION_FORM_20260806'
FROM tmp_import_1070_20260806_style target
WHERE target.style_id IS NULL;

UPDATE tmp_import_1070_20260806_style target
SET target.style_id = (
  SELECT style.id FROM competition_style_config style
  WHERE style.competition_id = 1070
    AND style.name = target.style_name
  ORDER BY style.active_flag DESC, style.id DESC LIMIT 1
);

CREATE TEMPORARY TABLE tmp_import_1070_20260806_entry (
  uuid CHAR(36) PRIMARY KEY,
  brewery_key VARCHAR(32) NOT NULL,
  category_id BIGINT NOT NULL,
  entry_name VARCHAR(255) NOT NULL,
  style_name VARCHAR(255) NOT NULL,
  abv DECIMAL(5,2) NOT NULL,
  ingredients TEXT NOT NULL,
  promotion VARCHAR(16) NOT NULL,
  extra_fields_json TEXT NOT NULL,
  full_code VARCHAR(32) NOT NULL,
  short_code VARCHAR(5) NOT NULL UNIQUE,
  label_code VARCHAR(64) NOT NULL,
  scan_token VARCHAR(128) NOT NULL,
  brewery_id BIGINT NULL,
  style_id BIGINT NULL,
  entry_id BIGINT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

INSERT INTO tmp_import_1070_20260806_entry
  (uuid, brewery_key, category_id, entry_name, style_name, abv, ingredients, promotion,
   extra_fields_json, full_code, short_code, label_code, scan_token) VALUES
('0e2da54a-fb84-4a75-8595-4c7a847e3175','xinghui',1593,CONVERT(0xe5ada4e5b29b USING utf8mb4),CONVERT(0xe5a2a8e8a5bfe593a5e78e89e7b1b3e68b89e6a0bc USING utf8mb4),4.60,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'6042909422091','22091','BE-IMP1070-ADD-AUG06-001','Q77BDFE24AD68F83886F832D20DE7D8E8D7C55B7E2A67E9F072721915D63CBAB'),
('de49a7e1-b3f5-4605-8e61-52e2b6b451b6','xinghui',1593,CONVERT(0xe5af92e6baaa USING utf8mb4),CONVERT(0xe4bca0e7bb9fe5beb7e5bc8fe79aaee5b094e6a3ae USING utf8mb4),4.70,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'6042909422092','22092','BE-IMP1070-ADD-AUG06-002','Q2A0500D60AE6A95213303774CCAFFE9DECD42B2E250EDDDEBA50B2F1CE91BED'),
('62fcbc8c-dbfc-4bcc-8e00-c133103a6d6b','xinghui',1593,CONVERT(0xe6999ae7a8bb USING utf8mb4),CONVERT(0xe5a4a7e7b1b3e68b89e6a0bc USING utf8mb4),5.00,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'6042909422093','22093','BE-IMP1070-ADD-AUG06-003','QCDD183E0512576F4A3DE796E10564B7E2449B7CB1014A701471B2915873A7AB'),
('85b9461b-fe98-499e-afee-e9df487a5ef4','xinghui',1593,CONVERT(0xe98791e5b883e68b89e6a0bc USING utf8mb4),CONVERT(0xe68db7e5858be6b7a1e889b2e68b89e6a0bc USING utf8mb4),4.30,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'6042909422094','22094','BE-IMP1070-ADD-AUG06-004','Q0FEF8D882742BB5093849470C9C16223740FBE88C2D2F70463C65D6AD5E80DB'),
('c571e76f-4373-4e98-81b8-2af863c12a78','xinghui',1593,CONVERT(0xe58d97e7baac3431e5baa6 USING utf8mb4),CONVERT(0xe98592e88ab1e68b89e6a0bc USING utf8mb4),5.70,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'6042909422095','22095','BE-IMP1070-ADD-AUG06-005','Q6B3BAD46676E4A61D6557E7474FD677422E1255F410D1FD8131A224EBE9517A'),
('c02a1f23-6519-48ec-ad80-bb48ddeb8a8d','xinghui',1593,CONVERT(0xe887aae794b1e4b98be8b7af USING utf8mb4),CONVERT(0xe98592e88ab1e68b89e6a0bc USING utf8mb4),5.70,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'6042909422096','22096','BE-IMP1070-ADD-AUG06-006','Q7E1AB8124FDC5427A1CE8EF175A5E2C75D60AA65457A76855E5CBFD3406CD77'),
('72e699c9-c55b-4ff9-a2a6-efa4f3b77e32','xinghui',1595,CONVERT(0xe5a986e5a986e4b881 USING utf8mb4),CONVERT(0xe6a48de789a9e5a29ee591b3e5889be6848fe68b89e6a0bc USING utf8mb4),4.60,CONVERT(0xe892b2e585ace88bb1e6a0b9e5a29ee591b3 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e892b2e585ace88bb1e6a0b9e5a29ee591b3222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'6042909422097','22097','BE-IMP1070-ADD-AUG06-007','QC08FBB170F79047BB1F2172732DE33BE299D2F87A9D512516FACF85F6D9E070'),
('c2c38baf-a41f-4052-9533-cec8c458143f','zhizaofu',1593,CONVERT(0xe8bf8ee698a5 USING utf8mb4),CONVERT(0xe697a5e5bc8fe5a4a7e7b1b3e68b89e6a0bc USING utf8mb4),4.90,CONVERT(0xe7b38ae58c96e8bf87e7a88be4b8ade5b091e9878fe6b7bbe58aa0e697a5e69cace696b0e6bd9fe58ebfe9b1bce6b2bce8b68ae58589e7b1b3efbc8ce9bb8fe680a7e69e81e5bcbae38081e887aae5b8a6e5a4a9e784b6e6b885e9a699efbc8ce4b88ee9baa6e6b181e89e8de59088e5baa6e69e81e9ab98efbc8ce4bdbfe98592e4bd93e69bb4e8bdbbe79b88e79a84e5908ce697b6e8b58be4ba88e4ba86e8bf99e6acbee68b89e6a0bce7949ce6b6a6e79a84e58fa3e6849fe38082e690ade9858de985b5e5af8ce5ae9ee9aa8ce5aea4e68f90e4be9be79a84e697a5e5bc8fe68b89e6a0bce4b893e794a8e985b5e6af8defbc8ce5b9b6e4bdbfe794a8e88ba6e9a699e585bce4bc98e38081e69ca8e8b4a8e88ab1e9a699e79a84e8b4b5e6978fe98592e88ab1efbc8ce5b8a6e69da5e6b281e4babae5bf83e884bee79a84e9b29ce788bde9a38ee591b3e38082 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e7b38ae58c96e8bf87e7a88be4b8ade5b091e9878fe6b7bbe58aa0e697a5e69cace696b0e6bd9fe58ebfe9b1bce6b2bce8b68ae58589e7b1b3efbc8ce9bb8fe680a7e69e81e5bcbae38081e887aae5b8a6e5a4a9e784b6e6b885e9a699efbc8ce4b88ee9baa6e6b181e89e8de59088e5baa6e69e81e9ab98efbc8ce4bdbfe98592e4bd93e69bb4e8bdbbe79b88e79a84e5908ce697b6e8b58be4ba88e4ba86e8bf99e6acbee68b89e6a0bce7949ce6b6a6e79a84e58fa3e6849fe38082e690ade9858de985b5e5af8ce5ae9ee9aa8ce5aea4e68f90e4be9be79a84e697a5e5bc8fe68b89e6a0bce4b893e794a8e985b5e6af8defbc8ce5b9b6e4bdbfe794a8e88ba6e9a699e585bce4bc98e38081e69ca8e8b4a8e88ab1e9a699e79a84e8b4b5e6978fe98592e88ab1efbc8ce5b8a6e69da5e6b281e4babae5bf83e884bee79a84e9b29ce788bde9a38ee591b3e38082222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'1092610749381','49381','BE-IMP1070-ADD-AUG06-008','Q1EC485E17A1F8E54469B4584CFA34DEA71749498DC58E6E5830D02A39C8D823'),
('751c3d22-5f26-46e1-ba81-22d60d38e69e','zhizaofu',1593,CONVERT(0xe68ea2e698a5 USING utf8mb4),CONVERT(0xe6b3a2e8a5bfe7b1b3e4ba9ae79aaee5b094e6a3ae USING utf8mb4),4.90,CONVERT(0xe69c80e7baafe6ada3e79a84e4b98ce5a58ee5b094e79aaee5b094e6a3aee88f8ce6a0aaefbc8c31303025e68db7e5858be79aaee5b094e6a3aee9baa6e88abde59fbae5ba95efbc8ce690ade9858de890a8e585b9e5b8a6e69da5e4bc98e99b85e79a84e88d89e69cace9a699e69699e38081e89c82e89c9ce5928ce6b3a5e59c9fe6b094e681afefbc8ce585a5e58fa3e98687e9a699e788bde88486efbc8ce698afe887b4e695ace6aca7e6b4b2e7bb8fe585b8e79aaee5b094e6a3aee9a38ee6a0bce79a84e69599e7a791e4b9a6e7baa7e4bdb3e4bd9ce38082 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e69c80e7baafe6ada3e79a84e4b98ce5a58ee5b094e79aaee5b094e6a3aee88f8ce6a0aaefbc8c31303025e68db7e5858be79aaee5b094e6a3aee9baa6e88abde59fbae5ba95efbc8ce690ade9858de890a8e585b9e5b8a6e69da5e4bc98e99b85e79a84e88d89e69cace9a699e69699e38081e89c82e89c9ce5928ce6b3a5e59c9fe6b094e681afefbc8ce585a5e58fa3e98687e9a699e788bde88486efbc8ce698afe887b4e695ace6aca7e6b4b2e7bb8fe585b8e79aaee5b094e6a3aee9a38ee6a0bce79a84e69599e7a791e4b9a6e7baa7e4bdb3e4bd9ce38082222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'1092610749382','49382','BE-IMP1070-ADD-AUG06-009','Q2ED1D12187347A7F095AFCCF85229343959A17159D3A3B88EE057A178E4309C'),
('e00249ea-1687-4044-b27f-c08d9cd178fb','zhizaofu',1593,CONVERT(0xe58583e698a5 USING utf8mb4),CONVERT(0xe4b8ade5bc8fe68b89e6a0bc USING utf8mb4),4.90,CONVERT(0xe4bdbfe794a831393033e5b9b4e794b1e5beb7e59bbde985bfe98592e5b888e5bc95e585a5e4b8ade59bbde5b9b6e7bb8fe8bf87e5b482e5b1b1e6b0b4e799bee5b9b4e585bbe882b2e79a84e58886e7a6bbe985b5e6af8de88f8ce7a78defbc8ce690ade9858de59388e68b89e98193e4baa7e58cbae4b8a4e6acbee88d89e69cace6b094e681afe9a6a5e98381efbc8ce9a38ee6a0bce58685e6959be79a84e98592e88ab1efbc8ce585b1e5908ce58f91e985b5e587bae8bf99e6acbee8b0b7e789a9e9a699e6b094e4b8b0e79b9be38081e795a5e5b8a6e99d92e88bb9e69e9ce985afe9a699e79a84e7a59ee7a798e4b8ade5bc8fe7bb8fe585b8e38082 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e4bdbfe794a831393033e5b9b4e794b1e5beb7e59bbde985bfe98592e5b888e5bc95e585a5e4b8ade59bbde5b9b6e7bb8fe8bf87e5b482e5b1b1e6b0b4e799bee5b9b4e585bbe882b2e79a84e58886e7a6bbe985b5e6af8de88f8ce7a78defbc8ce690ade9858de59388e68b89e98193e4baa7e58cbae4b8a4e6acbee88d89e69cace6b094e681afe9a6a5e98381efbc8ce9a38ee6a0bce58685e6959be79a84e98592e88ab1efbc8ce585b1e5908ce58f91e985b5e587bae8bf99e6acbee8b0b7e789a9e9a699e6b094e4b8b0e79b9be38081e795a5e5b8a6e99d92e88bb9e69e9ce985afe9a699e79a84e7a59ee7a798e4b8ade5bc8fe7bb8fe585b8e38082222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'1092610749383','49383','BE-IMP1070-ADD-AUG06-010','Q665A5E940128BB5F233731A877D9C65B18606E0C4434BE4E6578223E2546E92'),
('26da345e-ce57-452c-bef4-886a78ebd2c2','zhizaofu',1593,CONVERT(0xe6839ce698a5 USING utf8mb4),CONVERT(0xe5a2a8e8a5bfe593a5e68b89e6a0bc USING utf8mb4),5.00,CONVERT(0xe59d9ae69e9ce4b88ee5b9b2e69e9ce591b3e7a2b0e6929ee79a84e592b8e9b29ce9a699e6b094e698afe8bf99e6acbee5a2a8e8a5bfe593a5e68b89e6a0bce79a84e5a587e5a699e4b98be5a484e38082e5b091e9878f4d656c616e6fe9baa6e88abde5a29ee9a699efbc8ce7b396e58c96e4b8ade69c9fe6b7bbe58aa0e5a2a8e8a5bfe593a5e78e89e7b1b3e78987efbc8ce5b9b6e4bdbfe794a8e5a2a8e8a5bfe593a5e68b89e6a0bce4b893e794a8e985b5e6af8de58f91e985b5e38082e585a5e58fa3e5ae8ce585a8e4b88de4bcbce997bbe9a699e888ace5a48de69d82efbc8ce8be83e4bd8ee79a847048e78eafe5a283e5a191e980a0e587bae6b281e788bde79a84e99d92e69fa0e9a38ee591b3efbc8ce698afe5969de4b880e58fa3e5b0b1e5819ce4b88de4b88be69da5e79a84e4b896e7958ce69dafe5a5bde690ade6a1a3e38082 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e59d9ae69e9ce4b88ee5b9b2e69e9ce591b3e7a2b0e6929ee79a84e592b8e9b29ce9a699e6b094e698afe8bf99e6acbee5a2a8e8a5bfe593a5e68b89e6a0bce79a84e5a587e5a699e4b98be5a484e38082e5b091e9878f4d656c616e6fe9baa6e88abde5a29ee9a699efbc8ce7b396e58c96e4b8ade69c9fe6b7bbe58aa0e5a2a8e8a5bfe593a5e78e89e7b1b3e78987efbc8ce5b9b6e4bdbfe794a8e5a2a8e8a5bfe593a5e68b89e6a0bce4b893e794a8e985b5e6af8de58f91e985b5e38082e585a5e58fa3e5ae8ce585a8e4b88de4bcbce997bbe9a699e888ace5a48de69d82efbc8ce8be83e4bd8ee79a847048e78eafe5a283e5a191e980a0e587bae6b281e788bde79a84e99d92e69fa0e9a38ee591b3efbc8ce698afe5969de4b880e58fa3e5b0b1e5819ce4b88de4b88be69da5e79a84e4b896e7958ce69dafe5a5bde690ade6a1a3e38082222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'1092610749384','49384','BE-IMP1070-ADD-AUG06-011','Q45BBFA52ABD452CF600D6931BAE1E27D9E7F0BCAF7940086380F956D5B301B8'),
('e1f97ca6-e815-49f0-a996-b542486499d2','nanmen',1593,CONVERT(0xe4ba8ce4bb99e6a1a5e58d8ae5b9b2e8bdbbe7b2bee985bfe68b89e6a0bce595a4e98592 USING utf8mb4),CONVERT(0xe59bbde99985e6b7a1e889b2e68b89e6a0bc USING utf8mb4),3.50,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'973529589622','89622','BE-IMP1070-ADD-AUG06-012','QE64611CE514ADDE2E62D760FCD8EB2268A09D4BC8D4BFEA8C85844225E2CB39'),
('3455c0a1-66b6-4975-af32-db98a7a47d9d','nanmen',1593,CONVERT(0xe68890e983bde9baa6e98592 USING utf8mb4),CONVERT(0xe5beb7e5bc8fe6b885e4baaee68b89e6a0bc USING utf8mb4),5.00,CONVERT(0xe697a0 USING utf8mb4),CONVERT(0xe698af USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e697a0222c22637573746f6d5f313738353534393637373932315f3030383979223a22e698af227d USING utf8mb4),'943804643113','43113','BE-IMP1070-ADD-AUG06-013','Q498AB168303D37CCA362F32274C88CC2A4639137D59E57ED55189ED3A31A0DA'),
('7cad99b3-b758-44b4-af03-e5a835b6f97a','youhashu',1595,CONVERT(0xe69785e8b0b7 USING utf8mb4),CONVERT(0xe697a9e9a490e79aaee5b094e6a3ae USING utf8mb4),4.10,CONVERT(0xe8bf99e6acbee98592e58aa0e585a5e4ba86e59084e7b1bbe99da2e58c85efbc8ce9878ce99da2e4bc9ae69c89e59d9ae69e9cefbc8ce69e9ce5b9b2efbc8ce79b90e7ad89e7ad89efbc8ce68980e4bba5e7bb99e595a4e98592e5b8a6e69da5e4ba86e4b880e782b9e782b9e592b8e985b8e6849f USING utf8mb4),CONVERT(0xe590a6 USING utf8mb4),CONVERT(0x7b227370656369616c496e6772656469656e7473223a22e8bf99e6acbee98592e58aa0e585a5e4ba86e59084e7b1bbe99da2e58c85efbc8ce9878ce99da2e4bc9ae69c89e59d9ae69e9cefbc8ce69e9ce5b9b2efbc8ce79b90e7ad89e7ad89efbc8ce68980e4bba5e7bb99e595a4e98592e5b8a6e69da5e4ba86e4b880e782b9e782b9e592b8e985b8e6849f222c22637573746f6d5f313738353534393637373932315f3030383979223a22e590a6227d USING utf8mb4),'452894548819','48819','BE-IMP1070-ADD-AUG06-014','QF173151635A3A8209DA00D6BAB23C7E613690896292EC628E3B5BDE6B2C8D0E');

UPDATE tmp_import_1070_20260806_entry entry
JOIN tmp_import_1070_20260806_brewery brewery ON brewery.brewery_key = entry.brewery_key
JOIN tmp_import_1070_20260806_style style ON style.style_name = entry.style_name
SET entry.brewery_id = brewery.brewery_id, entry.style_id = style.style_id;

CREATE TEMPORARY TABLE tmp_import_1070_20260806_assert (
  value INT NOT NULL CHECK (value = 0)
);

INSERT INTO tmp_import_1070_20260806_assert
SELECT COUNT(*) FROM tmp_import_1070_20260806_entry
WHERE brewery_id IS NULL OR style_id IS NULL;

INSERT INTO tmp_import_1070_20260806_assert
SELECT COUNT(*)
FROM entry_scan_label label
JOIN beer_entry entry ON entry.id = label.beer_entry_id
WHERE label.competition_id = 1070
  AND label.short_code IN ('22091','22092','22093','22094','22095','22096','22097','49381','49382','49383','49384','89622','43113','48819')
  AND entry.uuid NOT IN ('0e2da54a-fb84-4a75-8595-4c7a847e3175','de49a7e1-b3f5-4605-8e61-52e2b6b451b6','62fcbc8c-dbfc-4bcc-8e00-c133103a6d6b','85b9461b-fe98-499e-afee-e9df487a5ef4','c571e76f-4373-4e98-81b8-2af863c12a78','c02a1f23-6519-48ec-ad80-bb48ddeb8a8d','72e699c9-c55b-4ff9-a2a6-efa4f3b77e32','c2c38baf-a41f-4052-9533-cec8c458143f','751c3d22-5f26-46e1-ba81-22d60d38e69e','e00249ea-1687-4044-b27f-c08d9cd178fb','26da345e-ce57-452c-bef4-886a78ebd2c2','e1f97ca6-e815-49f0-a996-b542486499d2','3455c0a1-66b6-4975-af32-db98a7a47d9d','7cad99b3-b758-44b4-af03-e5a835b6f97a');

INSERT INTO beer_entry
  (uuid, competition_id, brewery_id, registration_batch_id, category_id,
   name, style, style_config_id, abv, extra_fields_json, status, stored_flag, deleted_flag)
SELECT target.uuid, 1070, target.brewery_id, NULL, target.category_id,
       target.entry_name, target.style_name, target.style_id, target.abv,
       target.extra_fields_json, 'REGISTERED', 0, 0
FROM tmp_import_1070_20260806_entry target
WHERE NOT EXISTS (
  SELECT 1 FROM beer_entry existing
  WHERE existing.competition_id = 1070
    AND existing.uuid = target.uuid AND existing.deleted_flag = 0
);

UPDATE tmp_import_1070_20260806_entry target
JOIN beer_entry entry ON entry.competition_id = 1070
  AND entry.uuid = target.uuid AND entry.deleted_flag = 0
SET target.entry_id = entry.id;

UPDATE beer_entry entry
JOIN tmp_import_1070_20260806_entry target ON target.entry_id = entry.id
SET entry.brewery_id = target.brewery_id, entry.category_id = target.category_id,
    entry.name = target.entry_name, entry.style = target.style_name,
    entry.style_config_id = target.style_id, entry.abv = target.abv,
    entry.extra_fields_json = target.extra_fields_json,
    entry.status = 'REGISTERED', entry.stored_flag = 0, entry.deleted_flag = 0;

INSERT INTO entry_scan_label
  (competition_id, beer_entry_id, label_code, short_code, scan_token,
   status, generated_by, generated_time)
SELECT 1070, target.entry_id, target.label_code, target.short_code,
       target.scan_token, 'ACTIVE', NULL, NOW()
FROM tmp_import_1070_20260806_entry target
WHERE NOT EXISTS (
  SELECT 1 FROM entry_scan_label label
  WHERE label.beer_entry_id = target.entry_id AND label.status = 'ACTIVE'
);

INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT target.entry_id, config.field_key, config.field_label, target.ingredients
FROM tmp_import_1070_20260806_entry target
JOIN entry_field_config config ON config.competition_id = 1070
  AND config.field_key = 'specialIngredients' AND config.active_flag = 1
WHERE NOT EXISTS (
  SELECT 1 FROM beer_entry_extra_field existing
  WHERE existing.beer_entry_id = target.entry_id AND existing.field_key = config.field_key
);

INSERT INTO beer_entry_extra_field (beer_entry_id, field_key, field_label, field_value)
SELECT target.entry_id, config.field_key, config.field_label, target.promotion
FROM tmp_import_1070_20260806_entry target
JOIN entry_field_config config ON config.competition_id = 1070
  AND config.field_key = 'custom_1785549677921_0089y' AND config.active_flag = 1
WHERE NOT EXISTS (
  SELECT 1 FROM beer_entry_extra_field existing
  WHERE existing.beer_entry_id = target.entry_id AND existing.field_key = config.field_key
);

UPDATE beer_entry_extra_field extra
JOIN tmp_import_1070_20260806_entry target ON target.entry_id = extra.beer_entry_id
SET extra.field_value = CASE extra.field_key
  WHEN 'specialIngredients' THEN target.ingredients
  WHEN 'custom_1785549677921_0089y' THEN target.promotion
  ELSE extra.field_value END
WHERE extra.field_key IN ('specialIngredients','custom_1785549677921_0089y');

COMMIT;

SELECT label.short_code, entry.name, brewery.company_name, entry.style, entry.abv, label.status
FROM beer_entry entry
JOIN brewery brewery ON brewery.id = entry.brewery_id
JOIN entry_scan_label label ON label.beer_entry_id = entry.id
WHERE entry.competition_id = 1070
  AND entry.deleted_flag = 0 AND label.short_code IN ('22091','22092','22093','22094','22095','22096','22097','49381','49382','49383','49384','89622','43113','48819')
ORDER BY label.short_code;
