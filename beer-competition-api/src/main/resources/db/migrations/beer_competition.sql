/*
 Navicat Premium Data Transfer

 Source Server         : MySQL
 Source Server Type    : MySQL
 Source Server Version : 80100
 Source Host           : localhost:3306
 Source Schema         : beer_competition

 Target Server Type    : MySQL
 Target Server Version : 80100
 File Encoding         : 65001

 Date: 03/06/2026 11:59:03
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `admin_operation_log`;
CREATE TABLE `admin_operation_log`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint(0) NOT NULL,
  `action` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_public_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_admin_operation_log_target`(`target_type`, `target_public_id`) USING BTREE,
  INDEX `idx_admin_operation_log_admin`(`admin_user_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin_operation_log
-- ----------------------------
INSERT INTO `admin_operation_log` VALUES (1, 1, 'JUDGE_CONTACT_VIEW', 'JUDGE', 'J90FF0B37A47A', '查看评审完整联系方式', '2026-06-03 10:35:10');
INSERT INTO `admin_operation_log` VALUES (2, 1, 'JUDGE_CONTACT_VIEW', 'JUDGE', 'J90FF0B37A47A', '查看评审完整联系方式', '2026-06-03 10:35:18');
INSERT INTO `admin_operation_log` VALUES (3, 1, 'JUDGE_APPROVE_OR_ENABLE', 'JUDGE', 'J90FF0B37A47A', '评审状态改为启用', '2026-06-03 10:39:12');
INSERT INTO `admin_operation_log` VALUES (4, 1, 'JUDGE_APPROVE_OR_ENABLE', 'JUDGE', 'J90FF0B37A47A', '评审状态改为启用', '2026-06-03 10:39:12');

-- ----------------------------
-- Table structure for admin_user
-- ----------------------------
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` tinyint(0) NOT NULL DEFAULT 1,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_admin_user_username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin_user
-- ----------------------------
INSERT INTO `admin_user` VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '?????', 1, '2026-06-02 21:39:35', '2026-06-02 21:39:35');

-- ----------------------------
-- Table structure for beer_entry
-- ----------------------------
DROP TABLE IF EXISTS `beer_entry`;
CREATE TABLE `beer_entry`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `short_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `competition_id` bigint(0) NOT NULL,
  `brewery_id` bigint(0) NOT NULL,
  `category_id` bigint(0) NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `style` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `abv` decimal(4, 1) NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `extra_fields_json` json NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `stored_flag` tinyint(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_beer_entry_uuid`(`uuid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of beer_entry
-- ----------------------------
INSERT INTO `beer_entry` VALUES (1, 'BC-2026-IPA-0001', 'A19K', 1, 1, 1, '????IPA', 'Double IPA', 7.5, '????????????????????', '{\"specialIngredients\": \"????\"}', 'REGISTERED', 1, '2026-06-02 21:39:35', '2026-06-02 21:39:35');

-- ----------------------------
-- Table structure for entry_payment
-- ----------------------------
DROP TABLE IF EXISTS `entry_payment`;
CREATE TABLE `entry_payment`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `beer_entry_id` bigint(0) NOT NULL,
  `amount` decimal(10, 2) NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `pay_method` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `out_trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `wechat_transaction_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `paid_time` datetime(0) NULL DEFAULT NULL,
  `confirmed_by_admin_id` bigint(0) NULL DEFAULT NULL,
  `confirm_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_entry_payment_entry`(`beer_entry_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'entry payment' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of entry_payment
-- ----------------------------
INSERT INTO `entry_payment` VALUES (1, 1, 299.00, 'PAID', 'MANUAL', NULL, NULL, '2026-06-02 21:45:00', 1, 'manual confirm', '2026-06-02 21:39:35', '2026-06-02 21:45:00');

-- ----------------------------
-- Table structure for entry_delivery
-- ----------------------------
DROP TABLE IF EXISTS `entry_delivery`;
CREATE TABLE `entry_delivery`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `beer_entry_id` bigint(0) NOT NULL,
  `delivery_method` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `carrier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tracking_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `delivery_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `delivery_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `submitted_time` datetime(0) NULL DEFAULT NULL,
  `received_time` datetime(0) NULL DEFAULT NULL,
  `received_by_admin_id` bigint(0) NULL DEFAULT NULL,
  `receive_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_entry_delivery_entry`(`beer_entry_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'entry delivery' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of entry_delivery
-- ----------------------------
INSERT INTO `entry_delivery` VALUES (1, 1, 'EXPRESS', 'SF', 'SF1234567890', 'sample delivered', 'RECEIVED', '2026-06-03 09:00:00', '2026-06-03 10:00:00', 1, 'received', '2026-06-02 21:39:35', '2026-06-03 10:00:00');

-- ----------------------------
-- Table structure for beer_entry_extra_field
-- ----------------------------
DROP TABLE IF EXISTS `beer_entry_extra_field`;
CREATE TABLE `beer_entry_extra_field`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `beer_entry_id` bigint(0) NOT NULL,
  `field_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `field_label` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `field_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of beer_entry_extra_field
-- ----------------------------
INSERT INTO `beer_entry_extra_field` VALUES (1, 1, 'specialIngredients', '?????????', '????');

-- ----------------------------
-- Table structure for brewery
-- ----------------------------
DROP TABLE IF EXISTS `brewery`;
CREATE TABLE `brewery`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `company_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `wechat` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of brewery
-- ----------------------------
INSERT INTO `brewery` VALUES (1, '????', '??', '13800000001', 'brewery-zhangsan', '2026-06-02 21:39:35', '2026-06-02 21:39:35');

-- ----------------------------
-- Table structure for competition
-- ----------------------------
DROP TABLE IF EXISTS `competition`;
CREATE TABLE `competition`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `edition` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `competition_date` date NOT NULL,
  `registration_start` datetime(0) NULL DEFAULT NULL,
  `registration_deadline` datetime(0) NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `entry_fee` decimal(10, 2) NOT NULL,
  `style_library_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'BJCP_2021_CN',
  `deleted_flag` tinyint(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_competition_code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of competition
-- ----------------------------
INSERT INTO `competition` VALUES (1, 'BC-2026', '2026????????', '????', '2026-08-18', '2026-06-01 10:00:00', '2026-07-31 23:59:59', 'REGISTRATION_OPEN', 299.00, 'BJCP_2021_CN', 0, '2026-06-02 21:39:35', '2026-06-02 21:39:35');
INSERT INTO `competition` VALUES (2, 'BC-20260820-WSW1C', '2026 新建精酿啤酒赛', '第一批次', '2026-08-20', '2026-06-10 10:00:00', '2026-07-30 18:00:00', 'DRAFT', 199.00, 'BJCP_2021_CN', 0, '2026-06-03 09:42:07', '2026-06-03 09:42:07');

-- ----------------------------
-- Table structure for competition_category
-- ----------------------------
DROP TABLE IF EXISTS `competition_category`;
CREATE TABLE `competition_category`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `competition_id` bigint(0) NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_competition_category_name`(`competition_id`, `name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of competition_category
-- ----------------------------
INSERT INTO `competition_category` VALUES (1, 1, 'IPA', 1, '2026-06-02 21:39:35');
INSERT INTO `competition_category` VALUES (2, 1, '??', 2, '2026-06-02 21:39:35');
INSERT INTO `competition_category` VALUES (3, 2, '浅色拉格', 0, '2026-06-03 09:42:07');
INSERT INTO `competition_category` VALUES (4, 2, '深色拉格', 1, '2026-06-03 09:42:07');
INSERT INTO `competition_category` VALUES (5, 2, '创意拉格', 2, '2026-06-03 09:42:07');

-- ----------------------------
-- Table structure for competition_judge_assignment
-- ----------------------------
DROP TABLE IF EXISTS `competition_judge_assignment`;
CREATE TABLE `competition_judge_assignment`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `competition_id` bigint(0) NOT NULL,
  `base_table_id` bigint(0) NOT NULL,
  `judge_account_id` bigint(0) NOT NULL,
  `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_competition_judge_assignment`(`competition_id`, `judge_account_id`) USING BTREE,
  INDEX `idx_competition_judge_assignment_table`(`base_table_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of competition_judge_assignment
-- ----------------------------

-- ----------------------------
-- Table structure for competition_judge_table
-- ----------------------------
DROP TABLE IF EXISTS `competition_judge_table`;
CREATE TABLE `competition_judge_table`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `competition_id` bigint(0) NOT NULL,
  `table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_competition_judge_table_name`(`competition_id`, `table_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of competition_judge_table
-- ----------------------------
INSERT INTO `competition_judge_table` VALUES (1, 1, 'A?', 0, '2026-06-02 21:39:35', '2026-06-02 21:39:35');
INSERT INTO `competition_judge_table` VALUES (3, 1, 'A桌', 0, '2026-06-03 09:32:27', '2026-06-03 09:32:27');
INSERT INTO `competition_judge_table` VALUES (4, 2, 'A桌', 0, '2026-06-03 10:10:28', '2026-06-03 10:10:28');
INSERT INTO `competition_judge_table` VALUES (5, 2, 'B桌', 1, '2026-06-03 10:10:28', '2026-06-03 10:10:28');

-- ----------------------------
-- Table structure for competition_round
-- ----------------------------
DROP TABLE IF EXISTS `competition_round`;
CREATE TABLE `competition_round`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `competition_id` bigint(0) NOT NULL,
  `round_no` int(0) NOT NULL,
  `round_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `round_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `source_round_id` bigint(0) NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `published_time` datetime(0) NULL DEFAULT NULL,
  `submitted_time` datetime(0) NULL DEFAULT NULL,
  `locked_time` datetime(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_competition_round_no`(`competition_id`, `round_no`) USING BTREE,
  INDEX `idx_competition_round_source`(`source_round_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of competition_round
-- ----------------------------

-- ----------------------------
-- Table structure for competition_score_config
-- ----------------------------
DROP TABLE IF EXISTS `competition_score_config`;
CREATE TABLE `competition_score_config`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `competition_id` bigint(0) NOT NULL,
  `judge_role_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `min_comment_length` int(0) NOT NULL DEFAULT 0,
  `dimensions_json` json NOT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_competition_score_config`(`competition_id`, `judge_role_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of competition_score_config
-- ----------------------------
INSERT INTO `competition_score_config` VALUES (1, 1, 'CROSS', 0, '[{\"key\": \"impression\", \"label\": \"整体感受\", \"maxScore\": 20}, {\"key\": \"drinkability\", \"label\": \"适饮性\", \"maxScore\": 20}, {\"key\": \"story\", \"label\": \"记忆点\", \"maxScore\": 10}]', '2026-06-02 21:39:35', '2026-06-03 09:32:27');
INSERT INTO `competition_score_config` VALUES (2, 1, 'PROFESSIONAL', 0, '[{\"key\": \"aroma\", \"label\": \"香气\", \"maxScore\": 12}, {\"key\": \"appearance\", \"label\": \"外观\", \"maxScore\": 3}, {\"key\": \"flavor\", \"label\": \"味道\", \"maxScore\": 20}, {\"key\": \"mouthfeel\", \"label\": \"口感\", \"maxScore\": 5}, {\"key\": \"overall\", \"label\": \"整体印象\", \"maxScore\": 10}]', '2026-06-02 21:39:35', '2026-06-03 09:33:56');
INSERT INTO `competition_score_config` VALUES (3, 1, 'CAPTAIN', 0, '[{\"key\": \"consensus\", \"label\": \"共识评分\", \"maxScore\": 50}]', '2026-06-02 21:39:35', '2026-06-03 09:33:56');
INSERT INTO `competition_score_config` VALUES (10, 2, 'CROSS', 50, '[{\"key\": \"cross_1\", \"label\": \"整体感受\", \"score\": null, \"maxScore\": 20, \"notePrompt\": \"描述第一印象、愉悦度和整体完成度。\"}, {\"key\": \"cross_2\", \"label\": \"适饮性\", \"score\": null, \"maxScore\": 20, \"notePrompt\": \"描述是否易饮、是否愿意继续饮用。\"}, {\"key\": \"cross_3\", \"label\": \"记忆点\", \"score\": null, \"maxScore\": 10, \"notePrompt\": \"描述最容易被记住的风味或体验。\"}]', '2026-06-03 09:42:07', '2026-06-03 09:42:07');
INSERT INTO `competition_score_config` VALUES (11, 2, 'PROFESSIONAL', 30, '[{\"key\": \"professional_1\", \"label\": \"香气\", \"score\": null, \"maxScore\": 12, \"notePrompt\": \"描述香气强度、干净度和主要香气来源。\"}, {\"key\": \"professional_2\", \"label\": \"外观\", \"score\": null, \"maxScore\": 3, \"notePrompt\": \"描述颜色、清澈度、泡沫和持久性。\"}, {\"key\": \"professional_3\", \"label\": \"味道\", \"score\": null, \"maxScore\": 20, \"notePrompt\": \"描述甜苦酸平衡、风味层次和缺陷。\"}, {\"key\": \"professional_4\", \"label\": \"口感\", \"score\": null, \"maxScore\": 5, \"notePrompt\": \"描述酒体、杀口感、顺滑度和余味。\"}, {\"key\": \"professional_5\", \"label\": \"整体印象\", \"score\": null, \"maxScore\": 10, \"notePrompt\": \"汇总整体表现、完成度和改进建议。\"}]', '2026-06-03 09:42:07', '2026-06-03 09:42:07');
INSERT INTO `competition_score_config` VALUES (12, 2, 'CAPTAIN', 30, '[{\"key\": \"captain_1\", \"label\": \"共识评分\", \"score\": null, \"maxScore\": 50, \"notePrompt\": \"桌长讨论后填写独立共识分。\"}]', '2026-06-03 09:42:07', '2026-06-03 09:42:07');

-- ----------------------------
-- Table structure for competition_style_config
-- ----------------------------
DROP TABLE IF EXISTS `competition_style_config`;
CREATE TABLE `competition_style_config`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `competition_id` bigint(0) NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `style_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_competition_style_name`(`competition_id`, `name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of competition_style_config
-- ----------------------------
INSERT INTO `competition_style_config` VALUES (1, 1, 'American IPA', 'IPA', '21A', NULL, 1, '2026-06-02 21:39:35');
INSERT INTO `competition_style_config` VALUES (2, 1, 'Double IPA', 'IPA', '22A', NULL, 2, '2026-06-02 21:39:35');
INSERT INTO `competition_style_config` VALUES (3, 1, 'Pilsner', '拉格', '5D', NULL, 3, '2026-06-02 21:39:35');
INSERT INTO `competition_style_config` VALUES (6, 2, 'American IPA', 'IPA', '21A', NULL, 0, '2026-06-03 09:42:07');
INSERT INTO `competition_style_config` VALUES (7, 2, 'Double IPA', 'IPA', '22A', NULL, 1, '2026-06-03 09:42:07');
INSERT INTO `competition_style_config` VALUES (8, 2, 'Pilsner', '拉格', '5D', NULL, 2, '2026-06-03 09:42:07');
INSERT INTO `competition_style_config` VALUES (9, 2, 'Imperial Stout', '世涛与酸啤', '20C', NULL, 3, '2026-06-03 09:42:07');
INSERT INTO `competition_style_config` VALUES (10, 2, 'Mixed Fermentation Sour', '世涛与酸啤', '28B', NULL, 4, '2026-06-03 09:42:07');
INSERT INTO `competition_style_config` VALUES (11, 2, 'Gose', '世涛与酸啤', '27A', NULL, 5, '2026-06-03 09:42:07');
INSERT INTO `competition_style_config` VALUES (12, 2, 'Helles', '拉格', '4A', NULL, 6, '2026-06-03 09:42:07');
INSERT INTO `competition_style_config` VALUES (13, 2, 'Saison', 'IPA', '25B', NULL, 7, '2026-06-03 09:42:07');

-- ----------------------------
-- Table structure for entry_field_config
-- ----------------------------
DROP TABLE IF EXISTS `entry_field_config`;
CREATE TABLE `entry_field_config`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `competition_id` bigint(0) NOT NULL,
  `field_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `field_label` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `field_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `help_text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `options_json` json NULL,
  `required_flag` tinyint(0) NOT NULL DEFAULT 0,
  `visible_to_judges` tinyint(0) NOT NULL DEFAULT 0,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_entry_field_config_key`(`competition_id`, `field_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of entry_field_config
-- ----------------------------
INSERT INTO `entry_field_config` VALUES (1, 1, 'specialIngredients', '增味原料或特殊工艺', 'textarea', NULL, NULL, 0, 1, 1, '2026-06-02 21:39:35', '2026-06-03 09:33:56');
INSERT INTO `entry_field_config` VALUES (4, 2, 'specialIngredients', '增味原料或特殊工艺', 'textarea', '如使用茶、咖啡、水果、桶陈等，请描述原料和工艺。', NULL, 0, 1, 0, '2026-06-03 09:42:07', '2026-06-03 09:42:07');

-- ----------------------------
-- Table structure for file_asset
-- ----------------------------
DROP TABLE IF EXISTS `file_asset`;
CREATE TABLE `file_asset`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `storage_provider` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `storage_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `public_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_asset
-- ----------------------------

-- ----------------------------
-- Table structure for judge_account
-- ----------------------------
DROP TABLE IF EXISTS `judge_account`;
CREATE TABLE `judge_account`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `public_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `phone_enc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `phone_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `phone_last4` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `wechat_enc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `qualification` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` tinyint(0) NOT NULL DEFAULT 1,
  `submitted_time` datetime(0) NULL DEFAULT NULL,
  `reviewed_time` datetime(0) NULL DEFAULT NULL,
  `reviewed_by` bigint(0) NULL DEFAULT NULL,
  `review_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_judge_account_public_id`(`public_id`) USING BTREE,
  UNIQUE INDEX `uk_judge_account_phone_hash`(`phone_hash`) USING BTREE,
  INDEX `idx_judge_account_phone_last4`(`phone_last4`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of judge_account
-- ----------------------------
INSERT INTO `judge_account` VALUES (1, 'J90FF0B37A47A', 'v1:Xb7fCY4o8SoGV5DG:uakiY69AHdbyjdVUPFeHBFQKYy2z/drJ9Bji', '9f302d6c6600693d90bc98abb1d5bbe2eb3680f4a9ab29d1aae36036fff8e824', '0012', 'v1:6K9C6cwl7AeQGD7r:ZTYoRQMeGovodqlREXsTO2VVGjSzF3I=', '李狗蛋', '学生', 1, '2026-06-03 10:33:44', '2026-06-03 10:39:13', 1, NULL, '2026-06-03 10:33:16', '2026-06-03 10:33:16');
INSERT INTO `judge_account` VALUES (2, 'JTEST26060301', 'v1:JIdqC+rPdT1zqYXp:+DUgpOKmB9EjB/WJeMgWoaay0xHhI+fTWrTb', '9cdb0172e335f85a5e4fcd2a111ef7ac314aeb28cb0b799a617046e2843d348c', '0001', 'v1:abUId5GsJ/ENOzje:FWwXZtCegglivXFvH3mwYa+fWm/o5FYrcOGhCkM=', '陈明轩', 'BJCP 认证评审，精酿品鉴经验 5 年', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (3, 'JTEST26060302', 'v1:3I45oFOTUW5aA222:mN1R434IqygAZpuPb+yXGnBYm1WT+bSqyQxQ', 'd5a8107c1810324d713528ae63d7229380b108b8fe6e7ebea276e1c6c2785373', '0002', 'v1:QkcbuYmmjlXVj3Ic:KJFPfqcjntxidg4wdOrXKEF99WaQv68U29LcFrk=', '王雅婷', '啤酒酿造师，熟悉 IPA 与拉格风格', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (4, 'JTEST26060303', 'v1:XPoSTNWMAgqLdwyo:M0aoV+1nrjmM60SyWlEyJ/2hekrIiMuvZZCs', 'e67c50f860e15e4f11df641f5c45f3f4bf9dff1aea39e4d4860d0a3c92e2832f', '0003', 'v1:brMngTnBYRSJokAl:nnfqooE8qsiCjMID1j6PJ1xsOZol1J+ABJrP9Mw=', '张志强', '酒类赛事志愿评审，擅长感官记录', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (5, 'JTEST26060304', 'v1:vvk7R4gfszbXrYIi:EIEsYg0r59HCObt0+o+sCDuU3HBb8CQObWnv', 'e9e65ea1000aae96db95e57fc63fe9fd522581a6de705bd0412fd8bd5babd1ca', '0004', 'v1:Gm3Q1xYGnAbX37NQ:dCfEMdPpCQ5FZjFoCvpJDMq6n0pN0X2w8i3BwBw=', '刘嘉豪', '高校食品专业背景，参与多场品鉴活动', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (6, 'JTEST26060305', 'v1:jzFaIkUodg6xr+mV:jBYor9jazLvzkp3PJMUnDQwZC2WlGFle6y0R', '838a5edf8ffc6acda1b63d94388c2cdbcb56f200f4a0120fa4421db873f6931c', '0005', 'v1:1MaWWwDjy1CNcFQT:UbI90oj+45Mu+pdaTcXigGt1L4D9KgOWMq0KYvY=', '黄思琪', '精酿门店主理人，熟悉本地酒款风格', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (7, 'JTEST26060306', 'v1:5dTgVMeKCtH/muoj:2/1/7kkEJLkXRYqL/D6sQPYn72aW3DEuolEe', '62486ee70206ac98cf24e01b8a9d5b7fa4d008f5cbf9f1308032d5aa3cf3ccab', '0006', 'v1:6Lz+fb03hmXf28nd:tB2TqPfWX8lTSdJoNVit/uuCYGzwwNwkrPMSmbc=', '赵俊杰', 'BJCP 认证评审，精酿品鉴经验 5 年', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (8, 'JTEST26060307', 'v1:NdSCXANbn4TgWAYc:cWsaIzseO4nWW9H6R413sbuBsgQDX3A3pAVq', '505a9824312b64ff680e53135cfa7e5bee37ded672b8bb655767ca441187cef3', '0007', 'v1:FqkaSVqhUu0lai+W:VpxZdL4SfAp54U8mr2D4PaIksxktumDTrGoATKk=', '周雨桐', '啤酒酿造师，熟悉 IPA 与拉格风格', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (9, 'JTEST26060308', 'v1:b9b5wIVI1BnCGlGH:9I3Aa4T7Ufp1wM2orcIQUpJUZsE7nFB470E3', '65ee5d5d9add4eeb79b5b61218adf9a2e961810ed928d8d4096a5207cbd6f60f', '0008', 'v1:XJ2HgtkCaTYmN92H:5OosaZrGu5xz0cP5PyArydzWWiORoXDSVhDlJOc=', '吴浩然', '酒类赛事志愿评审，擅长感官记录', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (10, 'JTEST26060309', 'v1:EGod8m3LN25WVins:YdfGL78SjRAHg+vJllRgk4u8DrjHyJS/e1FW', 'c82a7692bf09a21f2e8b277faef948185a9f6cafaca6956ab0113033e2e1e64f', '0009', 'v1:qqG4llJsDnncccTr:0PjMHelv7YkA5/7jgcB/VtyJ5+5Oji/nBGLP1l8=', '郑欣怡', '高校食品专业背景，参与多场品鉴活动', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (11, 'JTEST26060310', 'v1:K8MQ+OwcDNcRXSkL:sJubLljUKXrqnfwgJaRkiAAgo8mqaQy++q7d', 'bc05dc4005801ee76af8317aae102a0c3730dfcc9b4ef1c3d3eb61da4b1a7349', '0010', 'v1:wruYdTX/K4hO1C2d:6XIeFFb7bwUvDfSAg8/oA6UTYcFog5xUFnCQ3pc=', '冯子涵', '精酿门店主理人，熟悉本地酒款风格', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (12, 'JTEST26060311', 'v1:bY/eixVoO+ug7RKX:zhRLJqBemVMDwNXo2IMP0yKh+EV925Vv3GYW', '070f0edacd5e4318d23d6d07b7d43d8d0d42404b80ff30a3e2a04b3866b24304', '0011', 'v1:D/4O58liEYKULkTf:09CFwY9NFIqV8W7tHdKalaZGO4V/4SSlRqC5a7A=', '孙梓航', 'BJCP 认证评审，精酿品鉴经验 5 年', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (13, 'JTEST26060312', 'v1:g92qScYWnheqTLOM:g2e7rz3OKj40qBjt/ELNiy4vB6fLEF9tGria', '27c93ce3b3a38da810f7d0cbd193ae63387605ad17dd035aa6b0ab47439893b1', '0012', 'v1:nesNIyihgqDFcS01:ARQdqWQCJWOIe8wp5wxQxEkwVHjaFsw6znw+tmw=', '何文博', '啤酒酿造师，熟悉 IPA 与拉格风格', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (14, 'JTEST26060313', 'v1:EOVzMREcErEfHwr2:A/wRHu+FUZeJyPZCv7QhSTjc4ucduVlG46I7', '956808f05f7f7f9a179831b87e4ca3085289e481dbf6a3fc8d8c55cffc1bd642', '0013', 'v1:58mRd+nYYs8fBbYg:PuT8qUXLt5a9isSWgiI/GOJkljs/uQsS9yf6prY=', '高佳怡', '酒类赛事志愿评审，擅长感官记录', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (15, 'JTEST26060314', 'v1:vfn0LVA6x3+RdFb+:ULadSObNJDaDTT85TumI2VLYFt1t4Eh9K9WM', 'c97af10cb8cc5ade58b0f46df7f5ac09471be600730d8e6c7bead0cb6cfb650a', '0014', 'v1:vhCyngDnXOwfk+Hn:O1aYfSnmMIDN9YmC1gI5yMLx/M7KKgz/FCu+ewI=', '林宇航', '高校食品专业背景，参与多场品鉴活动', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (16, 'JTEST26060315', 'v1:cdhGQVBME7BQURZn:EhZG90j2vTtWLQdDrXk3Tp4zvtCM92FNx8cl', '6484428746b1b75861cb3203ca313d35d3853fe7edf5916b89578ca46cfafd98', '0015', 'v1:J3Aa7SZiLObMEAEG:PkHsrmxe/4N2OkJMY1ZAzNOZDaos33Vd2R9f8kA=', '罗若曦', '精酿门店主理人，熟悉本地酒款风格', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (17, 'JTEST26060316', 'v1:fCxrFxYOjRWoRVmv:gNHMApyQR1Xi8PDEle7i91qQZkk57Pgj4sX8', 'fd1f7311f266fb98b15f65bd5f3167cd1d8acd53e25ba2da622affac71bf4554', '0016', 'v1:zbh6VplRY3XUtnqs:AuxGA1WJukohTxzBm7LrSD+/UEM+ognilwvRNRg=', '梁睿哲', 'BJCP 认证评审，精酿品鉴经验 5 年', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (18, 'JTEST26060317', 'v1:5yPzg+WqLU16Jhlu:WaGoDtXKNGQwJ550EkcRYx5L5H1FUF6sreRx', 'a8501aaea3a4b3d1d1f297110b360dc49fc727b0ca771bd316a070ee67d842f3', '0017', 'v1:2n0ZyNSPx5hGVoR8:QiQitXwOZGNhMUekY3nH2W1ZAt/tpGzREfWKfjQ=', '宋诗涵', '啤酒酿造师，熟悉 IPA 与拉格风格', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (19, 'JTEST26060318', 'v1:PVZBSRsp3UXgnf68:OyYG2bdv0t4+Z53FvZAfjnmvI1ybJjYjXSin', 'b43cc1685316c06c8cf3efe2bf60391224e932be1d65fa76844de00cd3379c17', '0018', 'v1:vBu5qmT2+4Daw6Wu:zGl51efniIhawiwReJKUV1RAEyRCLZFAKFrR2oc=', '唐一鸣', '酒类赛事志愿评审，擅长感官记录', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (20, 'JTEST26060319', 'v1:TT/xbzD2fOQ9pa0a:FFmn60Z3+z17wlNdJSEfrKqohblur1DqthiA', 'aac75805d0eab15c6e1c930a81f95a4660fa8d0c9b4cb9b234841a23cabb0699', '0019', 'v1:wOxoQ6kTeRVdsRKb:aon75Q17laoferkf868FkTyn+m0qJqNTAqowP5c=', '许晨曦', '高校食品专业背景，参与多场品鉴活动', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');
INSERT INTO `judge_account` VALUES (21, 'JTEST26060320', 'v1:CTcyhWH4GWUBO4W1:CQUqFk2yUDh/XICKqS7dfTzCrgGBMFCu+ltH', '8665baf948f04abcfeedbf5a894f1c1eae62893f2da9bb4712426cfe4ff94433', '0020', 'v1:aquLPER+FO1pOC1h:h8bLxd+mRIRT6GfyzztUGZ/AxTkH4MA1ID1hMVw=', '邓承泽', '精酿门店主理人，熟悉本地酒款风格', 1, '2026-06-03 10:40:00', '2026-06-03 10:40:00', 1, '测试账号，已启用', '2026-06-03 10:40:00', '2026-06-03 10:45:55');

-- ----------------------------
-- Table structure for portal_account
-- ----------------------------
DROP TABLE IF EXISTS `portal_account`;
CREATE TABLE `portal_account`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `wechat` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `display_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `brewery_id` bigint(0) NOT NULL,
  `status` tinyint(0) NOT NULL DEFAULT 1,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_portal_account_phone`(`phone`) USING BTREE,
  INDEX `idx_portal_account_brewery`(`brewery_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of portal_account
-- ----------------------------
INSERT INTO `portal_account` VALUES (1, '13800000001', 'brewery-zhangsan', '???????', 1, 1, '2026-06-02 21:39:35', '2026-06-02 21:39:35');

-- ----------------------------
-- Table structure for round_result
-- ----------------------------
DROP TABLE IF EXISTS `round_result`;
CREATE TABLE `round_result`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `competition_id` bigint(0) NOT NULL,
  `round_id` bigint(0) NOT NULL,
  `round_table_id` bigint(0) NOT NULL,
  `beer_entry_id` bigint(0) NOT NULL,
  `result_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `rank_no` int(0) NULL DEFAULT NULL,
  `slot_label` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `submitted_by` bigint(0) NULL DEFAULT NULL,
  `submitted_time` datetime(0) NULL DEFAULT NULL,
  `locked_flag` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_round_result_slot`(`round_table_id`, `result_type`, `rank_no`) USING BTREE,
  INDEX `idx_round_result_entry`(`beer_entry_id`) USING BTREE,
  INDEX `idx_round_result_competition`(`competition_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of round_result
-- ----------------------------

-- ----------------------------
-- Table structure for round_table
-- ----------------------------
DROP TABLE IF EXISTS `round_table`;
CREATE TABLE `round_table`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `competition_id` bigint(0) NOT NULL,
  `round_id` bigint(0) NOT NULL,
  `table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `captain_judge_id` bigint(0) NULL DEFAULT NULL,
  `category_id` bigint(0) NULL DEFAULT NULL,
  `category_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'EMPTY',
  `target_count` int(0) NOT NULL DEFAULT 1,
  `target_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_round_table_name`(`round_id`, `table_name`) USING BTREE,
  INDEX `idx_round_table_competition`(`competition_id`) USING BTREE,
  INDEX `idx_round_table_captain`(`captain_judge_id`) USING BTREE,
  INDEX `idx_round_table_category`(`category_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of round_table
-- ----------------------------

-- ----------------------------
-- Table structure for round_table_entry
-- ----------------------------
DROP TABLE IF EXISTS `round_table_entry`;
CREATE TABLE `round_table_entry`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `competition_id` bigint(0) NOT NULL,
  `round_id` bigint(0) NOT NULL,
  `round_table_id` bigint(0) NOT NULL,
  `beer_entry_id` bigint(0) NOT NULL,
  `source_round_table_id` bigint(0) NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_round_entry_once`(`round_id`, `beer_entry_id`) USING BTREE,
  INDEX `idx_round_table_entry_table`(`round_table_id`) USING BTREE,
  INDEX `idx_round_table_entry_competition`(`competition_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of round_table_entry
-- ----------------------------

-- ----------------------------
-- Table structure for round_table_member
-- ----------------------------
DROP TABLE IF EXISTS `round_table_member`;
CREATE TABLE `round_table_member`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `round_table_id` bigint(0) NOT NULL,
  `judge_account_id` bigint(0) NOT NULL,
  `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `system_task_required` tinyint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_round_table_member`(`round_table_id`, `judge_account_id`) USING BTREE,
  INDEX `idx_round_table_member_judge`(`judge_account_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of round_table_member
-- ----------------------------

-- ----------------------------
-- Table structure for score_record
-- ----------------------------
DROP TABLE IF EXISTS `score_record`;
CREATE TABLE `score_record`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `competition_id` bigint(0) NOT NULL,
  `beer_entry_id` bigint(0) NOT NULL,
  `judge_account_id` bigint(0) NOT NULL,
  `assignment_id` bigint(0) NULL DEFAULT NULL,
  `judge_role_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `dimensions_json` json NOT NULL,
  `total_score` decimal(6, 2) NOT NULL,
  `comments` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `is_final` tinyint(0) NOT NULL DEFAULT 0,
  `is_advanced` tinyint(0) NOT NULL DEFAULT 0,
  `consensus_score` decimal(6, 2) NULL DEFAULT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of score_record
-- ----------------------------

-- ----------------------------
-- Table structure for sms_code_log
-- ----------------------------
DROP TABLE IF EXISTS `sms_code_log`;
CREATE TABLE `sms_code_log`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `phone_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `masked_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sms_code_log_phone_hash`(`phone_hash`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sms_code_log
-- ----------------------------

-- ----------------------------
-- Table structure for style_category
-- ----------------------------
DROP TABLE IF EXISTS `style_category`;
CREATE TABLE `style_category`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `library_id` bigint(0) NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_style_category_name`(`library_id`, `name`) USING BTREE,
  INDEX `idx_style_category_library`(`library_id`, `sort_order`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 165 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of style_category
-- ----------------------------
INSERT INTO `style_category` VALUES (1, 1, 'IPA', 1, '2026-06-02 19:16:27');
INSERT INTO `style_category` VALUES (8, 1, '拉格', 2, '2026-06-02 19:18:28');
INSERT INTO `style_category` VALUES (9, 1, '世涛与酸啤', 3, '2026-06-02 19:18:28');
INSERT INTO `style_category` VALUES (49, 2, '拉格专题', 1, '2026-06-02 22:13:28');
INSERT INTO `style_category` VALUES (50, 2, '特色增味', 2, '2026-06-02 22:13:28');
INSERT INTO `style_category` VALUES (132, 3, '1. STANDARD AMERICAN BEER', 1, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (133, 3, '2. INTERNATIONAL LAGER', 2, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (134, 3, '3. CZECH LAGER', 3, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (135, 3, '4. PALE MALTY EUROPEAN LAGER', 4, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (136, 3, '5. PALE BITTER EUROPEAN BEER', 5, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (137, 3, '6. AMBER MALTY EUROPEAN LAGER', 6, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (138, 3, '7. AMBER BITTER EUROPEAN BEER', 7, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (139, 3, '8. DARK EUROPEAN LAGER', 8, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (140, 3, '9. STRONG EUROPEAN BEER', 9, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (141, 3, '10. GERMAN WHEAT BEER', 10, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (142, 3, '11. BRITISH BITTER', 11, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (143, 3, '12. PALE COMMONWEALTH BEER', 12, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (144, 3, '13. BROWN BRITISH BEER', 13, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (145, 3, '14. SCOTTISH ALE', 14, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (146, 3, '15. IRISH BEER', 15, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (147, 3, '16. DARK BRITISH BEER', 16, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (148, 3, '17. STRONG BRITISH ALE', 17, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (149, 3, '18. PALE AMERICAN ALE', 18, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (150, 3, '19. AMBER AND BROWN AMERICAN BEER', 19, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (151, 3, '20. AMERICAN PORTER AND STOUT', 20, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (152, 3, '21. IPA', 21, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (153, 3, '22. STRONG AMERICAN ALE', 22, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (154, 3, '23. EUROPEAN SOUR ALE', 23, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (155, 3, '24. BELGIAN ALE', 24, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (156, 3, '25. STRONG BELGIAN ALE', 25, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (157, 3, '26. MONASTIC ALE', 26, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (158, 3, '27. HISTORICAL BEER', 27, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (159, 3, '28. AMERICAN WILD ALE', 28, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (160, 3, '29. FRUIT BEER', 29, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (161, 3, '30. SPICED BEER', 30, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (162, 3, '31. ALTERNATIVE FERMENTABLES BEER', 31, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (163, 3, '32. SMOKED BEER', 32, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (164, 3, '33. WOOD BEER', 33, '2026-06-03 09:33:56');
INSERT INTO `style_category` VALUES (165, 3, '34. SPECIALTY BEER', 34, '2026-06-03 09:33:56');

-- ----------------------------
-- Table structure for style_item
-- ----------------------------
DROP TABLE IF EXISTS `style_item`;
CREATE TABLE `style_item`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `library_id` bigint(0) NOT NULL,
  `category_id` bigint(0) NULL DEFAULT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `style_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` tinyint(0) NOT NULL DEFAULT 1,
  `sort_order` int(0) NOT NULL DEFAULT 0,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_style_item_name`(`library_id`, `name`) USING BTREE,
  INDEX `idx_style_item_library`(`library_id`, `sort_order`) USING BTREE,
  INDEX `idx_style_item_category`(`category_id`, `sort_order`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 536 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of style_item
-- ----------------------------
INSERT INTO `style_item` VALUES (1, 1, 1, 'American IPA', '21A', NULL, 1, 1, '2026-06-02 19:16:27', '2026-06-02 19:16:27');
INSERT INTO `style_item` VALUES (2, 1, 1, 'Double IPA', '22A', NULL, 1, 2, '2026-06-02 19:16:27', '2026-06-02 19:16:27');
INSERT INTO `style_item` VALUES (3, 1, 8, 'Pilsner', '5D', NULL, 1, 3, '2026-06-02 19:16:27', '2026-06-03 09:32:27');
INSERT INTO `style_item` VALUES (4, 1, 9, 'Imperial Stout', '20C', NULL, 1, 4, '2026-06-02 19:16:27', '2026-06-03 09:32:27');
INSERT INTO `style_item` VALUES (5, 1, 9, 'Mixed Fermentation Sour', '28B', NULL, 1, 5, '2026-06-02 19:16:27', '2026-06-03 09:32:27');
INSERT INTO `style_item` VALUES (6, 1, 9, 'Gose', '27A', NULL, 1, 6, '2026-06-02 19:16:27', '2026-06-03 09:32:27');
INSERT INTO `style_item` VALUES (7, 1, 8, 'Helles', '4A', NULL, 1, 7, '2026-06-02 19:16:27', '2026-06-03 09:32:27');
INSERT INTO `style_item` VALUES (8, 1, 1, 'Saison', '25B', NULL, 1, 8, '2026-06-02 19:16:27', '2026-06-02 19:16:27');
INSERT INTO `style_item` VALUES (155, 2, 49, '浅色拉格', NULL, NULL, 1, 1, '2026-06-02 22:13:28', '2026-06-03 09:32:27');
INSERT INTO `style_item` VALUES (156, 2, 49, '深色拉格', NULL, NULL, 1, 2, '2026-06-02 22:13:28', '2026-06-03 09:32:27');
INSERT INTO `style_item` VALUES (157, 2, 49, '创意拉格', NULL, NULL, 1, 3, '2026-06-02 22:13:28', '2026-06-03 09:32:27');
INSERT INTO `style_item` VALUES (158, 2, 50, '水果酸啤', NULL, NULL, 1, 4, '2026-06-02 22:13:28', '2026-06-03 09:32:27');
INSERT INTO `style_item` VALUES (159, 2, 50, '桶陈世涛', NULL, NULL, 1, 5, '2026-06-02 22:13:28', '2026-06-03 09:32:27');
INSERT INTO `style_item` VALUES (161, 2, 50, '茶咖啡增味', NULL, NULL, 1, 6, '2026-06-02 22:13:28', '2026-06-02 22:13:28');
INSERT INTO `style_item` VALUES (163, 2, 50, '实验啤酒', NULL, NULL, 1, 7, '2026-06-02 22:13:28', '2026-06-03 09:32:27');
INSERT INTO `style_item` VALUES (421, 3, 132, 'American Light Lager', '1A', NULL, 1, 1, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (422, 3, 132, 'American Lager', '1B', NULL, 1, 2, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (423, 3, 132, 'Cream Ale', '1C', NULL, 1, 3, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (424, 3, 132, 'American Wheat Beer', '1D', NULL, 1, 4, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (425, 3, 133, 'International Pale Lager', '2A', NULL, 1, 5, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (426, 3, 133, 'International Amber Lager', '2B', NULL, 1, 6, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (427, 3, 133, 'International Dark Lager', '2C', NULL, 1, 7, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (428, 3, 134, 'Czech Pale Lager', '3A', NULL, 1, 8, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (429, 3, 134, 'Czech Premium Pale Lager', '3B', NULL, 1, 9, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (430, 3, 134, 'Czech Amber Lager', '3C', NULL, 1, 10, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (431, 3, 134, 'Czech Dark Lager', '3D', NULL, 1, 11, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (432, 3, 135, 'Munich Helles', '4A', NULL, 1, 12, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (433, 3, 135, 'Festbier', '4B', NULL, 1, 13, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (434, 3, 135, 'Helles Bock', '4C', NULL, 1, 14, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (435, 3, 136, 'German Leichtbier', '5A', NULL, 1, 15, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (436, 3, 136, 'Kölsch', '5B', NULL, 1, 16, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (437, 3, 136, 'German Helles Exportbier', '5C', NULL, 1, 17, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (438, 3, 136, 'German Pils', '5D', NULL, 1, 18, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (439, 3, 137, 'Märzen', '6A', NULL, 1, 19, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (440, 3, 137, 'Rauchbier', '6B', NULL, 1, 20, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (441, 3, 137, 'Dunkles Bock', '6C', NULL, 1, 21, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (442, 3, 138, 'Vienna Lager', '7A', NULL, 1, 22, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (443, 3, 138, 'Altbier', '7B', NULL, 1, 23, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (444, 3, 139, 'Munich Dunkel', '8A', NULL, 1, 24, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (445, 3, 139, 'Schwarzbier', '8B', NULL, 1, 25, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (446, 3, 140, 'Doppelbock', '9A', NULL, 1, 26, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (447, 3, 140, 'Eisbock', '9B', NULL, 1, 27, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (448, 3, 140, 'Baltic Porter', '9C', NULL, 1, 28, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (449, 3, 141, 'Weissbier', '10A', NULL, 1, 29, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (450, 3, 141, 'Dunkles Weissbier', '10B', NULL, 1, 30, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (451, 3, 141, 'Weizenbock', '10C', NULL, 1, 31, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (452, 3, 142, 'Ordinary Bitter', '11A', NULL, 1, 32, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (453, 3, 142, 'Best Bitter', '11B', NULL, 1, 33, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (454, 3, 142, 'Strong Bitter', '11C', NULL, 1, 34, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (455, 3, 143, 'British Golden Ale', '12A', NULL, 1, 35, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (456, 3, 143, 'Australian Sparkling Ale', '12B', NULL, 1, 36, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (457, 3, 143, 'English IPA', '12C', NULL, 1, 37, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (458, 3, 144, 'Dark Mild', '13A', NULL, 1, 38, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (459, 3, 144, 'British Brown Ale', '13B', NULL, 1, 39, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (460, 3, 144, 'English Porter', '13C', NULL, 1, 40, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (461, 3, 145, 'Scottish Light', '14A', NULL, 1, 41, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (462, 3, 145, 'Scottish Heavy', '14B', NULL, 1, 42, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (463, 3, 145, 'Scottish Export', '14C', NULL, 1, 43, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (464, 3, 146, 'Irish Red Ale', '15A', NULL, 1, 44, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (465, 3, 146, 'Irish Stout', '15B', NULL, 1, 45, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (466, 3, 146, 'Irish Extra Stout', '15C', NULL, 1, 46, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (467, 3, 147, 'Sweet Stout', '16A', NULL, 1, 47, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (468, 3, 147, 'Oatmeal Stout', '16B', NULL, 1, 48, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (469, 3, 147, 'Tropical Stout', '16C', NULL, 1, 49, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (470, 3, 147, 'Foreign Extra Stout', '16D', NULL, 1, 50, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (471, 3, 148, 'British Strong Ale', '17A', NULL, 1, 51, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (472, 3, 148, 'Old Ale', '17B', NULL, 1, 52, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (473, 3, 148, 'Wee Heavy', '17C', NULL, 1, 53, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (474, 3, 148, 'English Barleywine', '17D', NULL, 1, 54, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (475, 3, 149, 'Blonde Ale', '18A', NULL, 1, 55, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (476, 3, 149, 'American Pale Ale', '18B', NULL, 1, 56, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (477, 3, 150, 'American Amber Ale', '19A', NULL, 1, 57, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (478, 3, 150, 'California Common', '19B', NULL, 1, 58, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (479, 3, 150, 'American Brown Ale', '19C', NULL, 1, 59, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (480, 3, 151, 'American Porter', '20A', NULL, 1, 60, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (481, 3, 151, 'American Stout', '20B', NULL, 1, 61, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (482, 3, 151, 'Imperial Stout', '20C', NULL, 1, 62, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (483, 3, 152, 'American IPA', '21A', NULL, 1, 63, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (484, 3, 152, 'Specialty IPA', '21B', NULL, 1, 64, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (485, 3, 152, 'Hazy IPA', '21C', NULL, 1, 65, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (486, 3, 153, 'Double IPA', '22A', NULL, 1, 66, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (487, 3, 153, 'American Strong Ale', '22B', NULL, 1, 67, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (488, 3, 153, 'American Barleywine', '22C', NULL, 1, 68, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (489, 3, 153, 'Wheatwine', '22D', NULL, 1, 69, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (490, 3, 154, 'Berliner Weisse', '23A', NULL, 1, 70, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (491, 3, 154, 'Flanders Red Ale', '23B', NULL, 1, 71, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (492, 3, 154, 'Oud Bruin', '23C', NULL, 1, 72, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (493, 3, 154, 'Lambic', '23D', NULL, 1, 73, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (494, 3, 154, 'Gueuze', '23E', NULL, 1, 74, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (495, 3, 154, 'Fruit Lambic', '23F', NULL, 1, 75, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (496, 3, 154, 'Gose', '23G', NULL, 1, 76, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (497, 3, 155, 'Witbier', '24A', NULL, 1, 77, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (498, 3, 155, 'Belgian Pale Ale', '24B', NULL, 1, 78, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (499, 3, 155, 'Bière de Garde', '24C', NULL, 1, 79, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (500, 3, 156, 'Belgian Blond Ale', '25A', NULL, 1, 80, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (501, 3, 156, 'Saison', '25B', NULL, 1, 81, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (502, 3, 156, 'Belgian Golden Strong Ale', '25C', NULL, 1, 82, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (503, 3, 157, 'Belgian Single', '26A', NULL, 1, 83, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (504, 3, 157, 'Belgian Dubbel', '26B', NULL, 1, 84, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (505, 3, 157, 'Belgian Tripel', '26C', NULL, 1, 85, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (506, 3, 157, 'Belgian Dark Strong Ale', '26D', NULL, 1, 86, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (507, 3, 158, 'Kellerbier', '27A', NULL, 1, 87, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (508, 3, 158, 'Kentucky Common', '27B', NULL, 1, 88, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (509, 3, 158, 'Lichtenhainer', '27C', NULL, 1, 89, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (510, 3, 158, 'London Brown Ale', '27D', NULL, 1, 90, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (511, 3, 158, 'Piwo Grodziskie', '27E', NULL, 1, 91, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (512, 3, 158, 'Pre-Prohibition Lager', '27F', NULL, 1, 92, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (513, 3, 158, 'Pre-Prohibition Porter', '27G', NULL, 1, 93, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (514, 3, 158, 'Roggenbier', '27H', NULL, 1, 94, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (515, 3, 158, 'Sahti', '27I', NULL, 1, 95, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (516, 3, 159, 'Brett Beer', '28A', NULL, 1, 96, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (517, 3, 159, 'Mixed-Fermentation Sour Beer', '28B', NULL, 1, 97, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (518, 3, 159, 'Wild Specialty Beer', '28C', NULL, 1, 98, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (519, 3, 159, 'Straight Sour Beer', '28D', NULL, 1, 99, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (520, 3, 160, 'Fruit Beer', '29A', NULL, 1, 100, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (521, 3, 160, 'Fruit and Spice Beer', '29B', NULL, 1, 101, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (522, 3, 160, 'Specialty Fruit Beer', '29C', NULL, 1, 102, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (523, 3, 160, 'Grape Ale', '29D', NULL, 1, 103, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (524, 3, 161, 'Spice, Herb, or Vegetable Beer', '30A', NULL, 1, 104, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (525, 3, 161, 'Autumn Seasonal Beer', '30B', NULL, 1, 105, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (526, 3, 161, 'Winter Seasonal Beer', '30C', NULL, 1, 106, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (527, 3, 161, 'Specialty Spice Beer', '30D', NULL, 1, 107, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (528, 3, 162, 'Alternative Grain Beer', '31A', NULL, 1, 108, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (529, 3, 162, 'Alternative Sugar Beer', '31B', NULL, 1, 109, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (530, 3, 163, 'Classic Style Smoked Beer', '32A', NULL, 1, 110, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (531, 3, 163, 'Specialty Smoked Beer', '32B', NULL, 1, 111, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (532, 3, 164, 'Wood-Aged Beer', '33A', NULL, 1, 112, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (533, 3, 164, 'Specialty Wood-Aged Beer', '33B', NULL, 1, 113, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (534, 3, 165, 'Commercial Specialty Beer', '34A', NULL, 1, 114, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (535, 3, 165, 'Mixed-Style Beer', '34B', NULL, 1, 115, '2026-06-03 09:33:56', '2026-06-03 09:33:56');
INSERT INTO `style_item` VALUES (536, 3, 165, 'Experimental Beer', '34C', NULL, 1, 116, '2026-06-03 09:33:56', '2026-06-03 09:33:56');

-- ----------------------------
-- Table structure for style_library
-- ----------------------------
DROP TABLE IF EXISTS `style_library`;
CREATE TABLE `style_library`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `language` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `source` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` tinyint(0) NOT NULL DEFAULT 1,
  `tags_json` json NULL,
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_style_library_code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of style_library
-- ----------------------------
INSERT INTO `style_library` VALUES (1, 'BJCP_2021_CN', 'BJCP 2021 中文标准库', '2021', '中文', 'BJCP', 1, '[\"报名必填\", \"支持搜索\", \"评审可见\"]', '2026-06-02 19:16:27', '2026-06-03 09:32:27');
INSERT INTO `style_library` VALUES (2, 'CUSTOM_STANDARD', '主办方标准风格库', '2026A', '中文', '主办方', 1, '[\"自定义分类\", \"报名可搜\", \"评审可见\"]', '2026-06-02 19:16:27', '2026-06-03 09:32:27');
INSERT INTO `style_library` VALUES (3, 'BJCP_2021_EN', 'BJCP 2021 Beer Style Guidelines', '2021', 'English', 'BJCP', 1, '[\"Official directory\", \"English styles\", \"Competition-ready\"]', '2026-06-02 19:16:27', '2026-06-03 09:33:56');

SET FOREIGN_KEY_CHECKS = 1;
