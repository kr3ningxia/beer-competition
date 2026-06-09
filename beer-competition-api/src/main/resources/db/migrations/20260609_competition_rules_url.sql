ALTER TABLE `competition`
    ADD COLUMN `rules_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL AFTER `description`;
