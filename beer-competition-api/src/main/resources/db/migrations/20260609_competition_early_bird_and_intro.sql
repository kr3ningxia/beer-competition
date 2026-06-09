ALTER TABLE `competition`
    ADD COLUMN `early_bird_fee` decimal(10, 2) NULL DEFAULT NULL AFTER `entry_fee`,
    ADD COLUMN `early_bird_deadline` datetime(0) NULL DEFAULT NULL AFTER `early_bird_fee`,
    ADD COLUMN `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL AFTER `early_bird_deadline`;
