/*
 Navicat Premium Data Transfer

 Source Server         : MysqlJQPC
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : agricultural

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 28/02/2026 23:12:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for banner
-- ----------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '???ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??URL',
  `link_type` tinyint NULL DEFAULT 0 COMMENT '????: 0-? 1-???? 2-????',
  `link_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `sort` int NULL DEFAULT 0 COMMENT '??',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `start_time` datetime NULL DEFAULT NULL COMMENT '????',
  `end_time` datetime NULL DEFAULT NULL COMMENT '????',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blockchain_contract
-- ----------------------------
DROP TABLE IF EXISTS `blockchain_contract`;
CREATE TABLE `blockchain_contract`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `contract_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `contract_address` varchar(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `contract_abi` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '??ABI',
  `contract_code` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????',
  `deploy_tx_hash` varchar(66) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??????',
  `deploy_block_number` bigint NULL DEFAULT NULL COMMENT '??????',
  `network` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??(mainnet/testnet)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_contract_address`(`contract_address` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blockchain_gas_fee
-- ----------------------------
DROP TABLE IF EXISTS `blockchain_gas_fee`;
CREATE TABLE `blockchain_gas_fee`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `stat_date` date NOT NULL COMMENT '????',
  `enterprise_id` bigint NULL DEFAULT NULL COMMENT '??ID(NULL??????)',
  `transaction_count` int NULL DEFAULT 0 COMMENT '????',
  `total_gas_used` bigint NULL DEFAULT 0 COMMENT '???Gas',
  `total_fee_wei` decimal(30, 0) NULL DEFAULT 0 COMMENT '???(Wei)',
  `total_fee_eth` decimal(20, 8) NULL DEFAULT 0.00000000 COMMENT '???(ETH)',
  `avg_gas_price` bigint NULL DEFAULT 0 COMMENT '??Gas??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stat_date_enterprise`(`stat_date` ASC, `enterprise_id` ASC) USING BTREE,
  INDEX `idx_stat_date`(`stat_date` ASC) USING BTREE,
  INDEX `idx_enterprise_id`(`enterprise_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Gas?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blockchain_transaction
-- ----------------------------
DROP TABLE IF EXISTS `blockchain_transaction`;
CREATE TABLE `blockchain_transaction`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tx_hash` varchar(66) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `message_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??ID(??????)',
  `business_type` tinyint NOT NULL COMMENT '????: 1-?? 2-???? 3-?? 4-?? 5-?? 6-?? 7-??',
  `business_id` bigint NOT NULL COMMENT '????ID',
  `batch_id` bigint NOT NULL COMMENT '??ID',
  `data_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '数据哈希(SHA-256)',
  `contract_address` varchar(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `from_address` varchar(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `block_number` bigint NULL DEFAULT NULL COMMENT '????',
  `block_hash` varchar(66) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `gas_used` bigint NULL DEFAULT NULL COMMENT '??Gas',
  `gas_price` bigint NULL DEFAULT NULL COMMENT 'Gas??(Wei)',
  `transaction_fee` decimal(20, 8) NULL DEFAULT NULL COMMENT '????(ETH)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '??: 0-??? 1-?? 2-??',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????',
  `chain_time` datetime NULL DEFAULT NULL COMMENT '????',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tx_hash`(`tx_hash` ASC) USING BTREE,
  UNIQUE INDEX `uk_message_id`(`message_id` ASC) USING BTREE,
  INDEX `idx_business_type_id`(`business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_block_number`(`block_number` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '????????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for education_content
-- ----------------------------
DROP TABLE IF EXISTS `education_content`;
CREATE TABLE `education_content`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  `cover_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '??',
  `author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  `view_count` int NULL DEFAULT 0 COMMENT '????',
  `sort` int NULL DEFAULT 0 COMMENT '??',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for enterprise_info
-- ----------------------------
DROP TABLE IF EXISTS `enterprise_info`;
CREATE TABLE `enterprise_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `enterprise_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????(????????)',
  `enterprise_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `enterprise_type` tinyint NOT NULL COMMENT '????: 1-???? 2-???? 3-????',
  `legal_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `contact_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `contact_email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `business_license` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????URL',
  `production_license` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '?????URL',
  `other_certificates` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '??????(JSON??)',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????',
  `logo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??LOGO',
  `wallet_address` varchar(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???????(0x??)',
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '????: 0-??? 1-???? 2-????',
  `audit_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '????',
  `audit_by` bigint NULL DEFAULT NULL COMMENT '???',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_enterprise_code`(`enterprise_code` ASC) USING BTREE,
  INDEX `idx_enterprise_type`(`enterprise_type` ASC) USING BTREE,
  INDEX `idx_audit_status`(`audit_status` ASC) USING BTREE,
  INDEX `idx_wallet_address`(`wallet_address` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '???',
  `config_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '????ID',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `dict_sort` int NULL DEFAULT 0 COMMENT '??',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??????',
  `is_default` tinyint NULL DEFAULT 0 COMMENT '????: 0-? 1-?',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dict_type`(`dict_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 44 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '????ID',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dict_type`(`dict_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '??ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'IP??',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `browser` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???',
  `os` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `status` tinyint NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_login_time`(`login_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '???ID(0??????)',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `menu_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `menu_type` tinyint NOT NULL COMMENT '????: 1-?? 2-?? 3-??',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `sort` int NULL DEFAULT 0 COMMENT '??',
  `visible` tinyint NOT NULL DEFAULT 1 COMMENT '????: 0-?? 1-??',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_menu_code`(`menu_code` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1502 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '????ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '?????',
  `operation` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'IP??',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `browser` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???',
  `os` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `status` tinyint NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????',
  `execute_time` int NULL DEFAULT NULL COMMENT '????(??)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `role_type` tinyint NOT NULL COMMENT '????: 1-????? 2-?????? 3-?????? 4-??????',
  `sort` int NULL DEFAULT 0 COMMENT '??',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `role_id` bigint NOT NULL COMMENT '??ID',
  `menu_id` bigint NOT NULL COMMENT '??ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_menu`(`role_id` ASC, `menu_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 133 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??(??)',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??URL',
  `user_type` tinyint NOT NULL DEFAULT 1 COMMENT '????: 1-????? 2-???? 3-????',
  `enterprise_id` bigint NULL DEFAULT NULL COMMENT '????ID(??????)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '??????',
  `login_count` int NULL DEFAULT 0 COMMENT '????',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  INDEX `idx_enterprise_id`(`enterprise_id` ASC) USING BTREE,
  INDEX `idx_user_type`(`user_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `user_id` bigint NOT NULL COMMENT '??ID',
  `role_id` bigint NOT NULL COMMENT '??ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_batch
-- ----------------------------
DROP TABLE IF EXISTS `trace_batch`;
CREATE TABLE `trace_batch`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `batch_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????(?????, ????: batchNo)',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `product_type` tinyint NOT NULL COMMENT '????: 1-?? 2-??? (????: templateType)',
  `breed` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??(????: breed, ????/????)',
  `enterprise_id` bigint NOT NULL COMMENT '????ID',
  `qr_code_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '?????URL',
  `init_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '????(????: quantity, ????/??)',
  `current_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '????',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??(?/?/kg)',
  `origin_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????(????: origin)',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '????(??GPS????)',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '????(??GPS????)',
  `plant_area` decimal(10, 2) NULL DEFAULT NULL COMMENT '????/?(????: plantArea, ?????)',
  `greenhouse_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???(????: greenhouseNo, ?????)',
  `seed_source` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '鑻楁簮/绉嶅瓙鏉ユ簮',
  `manager` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???(????: manager)',
  `production_date` date NULL DEFAULT NULL COMMENT '??/????',
  `expected_harvest_date` date NULL DEFAULT NULL COMMENT '????/????',
  `actual_harvest_date` date NULL DEFAULT NULL COMMENT '????/????',
  `batch_status` tinyint NOT NULL DEFAULT 1 COMMENT '????: 1-??? 2-??? 3-??? 4-??? 5-??? 6-??? 7-??? 8-???',
  `tx_hash` varchar(66) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???????',
  `block_number` bigint NULL DEFAULT NULL COMMENT '????',
  `chain_time` datetime NULL DEFAULT NULL COMMENT '????',
  `data_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '数据哈希(SHA-256)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_batch_code`(`batch_code` ASC) USING BTREE,
  INDEX `idx_enterprise_id`(`enterprise_id` ASC) USING BTREE,
  INDEX `idx_product_type`(`product_type` ASC) USING BTREE,
  INDEX `idx_batch_status`(`batch_status` ASC) USING BTREE,
  INDEX `idx_tx_hash`(`tx_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 104 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_field
-- ----------------------------
DROP TABLE IF EXISTS `trace_field`;
CREATE TABLE `trace_field`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `stage_id` bigint NOT NULL COMMENT '????ID',
  `field_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `field_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????: text/number/date/select/image/video',
  `field_options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????(JSON??, ??select??)',
  `is_required` tinyint NOT NULL DEFAULT 0 COMMENT '????: 0-? 1-?',
  `sort` int NULL DEFAULT 0 COMMENT '??',
  `placeholder` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `default_value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???',
  `validation_rule` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_stage_id`(`stage_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_inspection
-- ----------------------------
DROP TABLE IF EXISTS `trace_inspection`;
CREATE TABLE `trace_inspection`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `batch_id` bigint NOT NULL COMMENT '??ID',
  `batch_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??????(????: batchCode)',
  `inspection_date` date NOT NULL COMMENT '????(????: inspectionDate)',
  `check_result` tinyint NULL DEFAULT NULL COMMENT '检疫结果: 1-合格 0-不合格 NULL-待审核',
  `inspection_items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '??????(????: inspectionItems, ??500?)',
  `cert_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??????(????: certificateNo)',
  `cert_image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '妫€鐤瘉涔﹀浘鐗?JSON鏁扮粍, 鏀寔澶氬紶, 鍓嶇瀛楁: certificateImages/imagePath)',
  `inspector` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '?????(????: inspector, ????)',
  `inspector_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '?????(????: inspectorCode, ????)',
  `inspector_id` bigint NULL DEFAULT NULL COMMENT '?????ID(??sys_user)',
  `inspection_enterprise_id` bigint NULL DEFAULT NULL COMMENT '????ID(??enterprise_info)',
  `tx_hash` varchar(66) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???????',
  `block_number` bigint NULL DEFAULT NULL COMMENT '????',
  `chain_time` datetime NULL DEFAULT NULL COMMENT '????',
  `data_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '数据哈希(SHA-256)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??(????: remark, ??200?)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_batch_code`(`batch_code` ASC) USING BTREE,
  INDEX `idx_check_result`(`check_result` ASC) USING BTREE,
  INDEX `idx_inspector_id`(`inspector_id` ASC) USING BTREE,
  INDEX `idx_inspection_enterprise_id`(`inspection_enterprise_id` ASC) USING BTREE,
  INDEX `idx_tx_hash`(`tx_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_processing
-- ----------------------------
DROP TABLE IF EXISTS `trace_processing`;
CREATE TABLE `trace_processing`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `batch_id` bigint NOT NULL COMMENT '??ID',
  `source_batch_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '?????(????: sourceBatchCode, ??/????)',
  `processing_enterprise_id` bigint NULL DEFAULT NULL COMMENT '????ID',
  `processing_date` date NOT NULL COMMENT '????(????: processingDate)',
  `process_method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????(????: processingMethod: ??/??/??)',
  `specs` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????(????: packagingSpec, ?500g/?)',
  `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???(????: operator)',
  `input_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '????(????: inputQuantity)',
  `output_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '????(????: outputQuantity)',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????(JSON??, ????: photoList, ??6?)',
  `tx_hash` varchar(66) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???????',
  `block_number` bigint NULL DEFAULT NULL COMMENT '????',
  `chain_time` datetime NULL DEFAULT NULL COMMENT '????',
  `data_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '数据哈希(SHA-256)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??(????: remark, ??200?)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_source_batch_code`(`source_batch_code` ASC) USING BTREE,
  INDEX `idx_processing_enterprise_id`(`processing_enterprise_id` ASC) USING BTREE,
  INDEX `idx_processing_date`(`processing_date` ASC) USING BTREE,
  INDEX `idx_tx_hash`(`tx_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_record
-- ----------------------------
DROP TABLE IF EXISTS `trace_record`;
CREATE TABLE `trace_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `batch_id` bigint NOT NULL COMMENT '??ID',
  `batch_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??????(??, ????)',
  `record_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????(????: recordType): feeding-?? vaccine-?? inspect-?? fertilize-?? irrigate-?? pesticide-??',
  `record_date` date NOT NULL COMMENT '????(????: recordDate)',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????(????: materialName): ??/??/??/????',
  `amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '??(????: dosage)',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????(????: description, ??500?)',
  `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '?????(????: operator)',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '??????(JSON??, ????: images, ??9?)',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????(??GPS??????)',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '????',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '????',
  `tx_hash` varchar(66) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???????',
  `block_number` bigint NULL DEFAULT NULL COMMENT '????',
  `chain_time` datetime NULL DEFAULT NULL COMMENT '????',
  `data_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '数据哈希(SHA-256)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_batch_code`(`batch_code` ASC) USING BTREE,
  INDEX `idx_record_type`(`record_type` ASC) USING BTREE,
  INDEX `idx_record_date`(`record_date` ASC) USING BTREE,
  INDEX `idx_tx_hash`(`tx_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '??/?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_sale
-- ----------------------------
DROP TABLE IF EXISTS `trace_sale`;
CREATE TABLE `trace_sale`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `batch_id` bigint NOT NULL COMMENT '??ID',
  `batch_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??????(????: batchNo)',
  `sale_enterprise_id` bigint NULL DEFAULT NULL COMMENT '????ID',
  `sale_date` date NOT NULL COMMENT '????(????: saleDate)',
  `sale_time` time NULL DEFAULT NULL COMMENT '????(????: saleTime)',
  `buyer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????(????: buyerName, ???/????)',
  `sale_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '????(????: quantity)',
  `sale_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  `sale_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '????/?(????: unitPrice)',
  `total_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '????/?(??????: unitPrice x quantity)',
  `sale_voucher` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '??????(JSON??, ????: invoiceImages, ??3?)',
  `sale_channel` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `destination` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '?????',
  `tx_hash` varchar(66) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???????',
  `block_number` bigint NULL DEFAULT NULL COMMENT '????',
  `chain_time` datetime NULL DEFAULT NULL COMMENT '????',
  `data_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '数据哈希(SHA-256)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_batch_code`(`batch_code` ASC) USING BTREE,
  INDEX `idx_sale_enterprise_id`(`sale_enterprise_id` ASC) USING BTREE,
  INDEX `idx_sale_date`(`sale_date` ASC) USING BTREE,
  INDEX `idx_tx_hash`(`tx_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_stage
-- ----------------------------
DROP TABLE IF EXISTS `trace_stage`;
CREATE TABLE `trace_stage`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `template_id` bigint NOT NULL COMMENT '????ID',
  `stage_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `stage_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `stage_type` tinyint NOT NULL COMMENT '????: 1-??? 2-???? 3-?? 4-?? 5-?? 6-?? 7-??',
  `sort` int NULL DEFAULT 0 COMMENT '??',
  `is_required` tinyint NOT NULL DEFAULT 1 COMMENT '????: 0-? 1-?',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_template_id`(`template_id` ASC) USING BTREE,
  INDEX `idx_stage_type`(`stage_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_storage
-- ----------------------------
DROP TABLE IF EXISTS `trace_storage`;
CREATE TABLE `trace_storage`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `batch_id` bigint NOT NULL COMMENT '??ID',
  `batch_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??????(????: batchCode)',
  `storage_enterprise_id` bigint NULL DEFAULT NULL COMMENT '????ID',
  `storage_type` tinyint NOT NULL COMMENT '????: 1-?? 2-?? 3-????',
  `storage_date` date NOT NULL COMMENT '????',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `warehouse_location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `storage_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '????(????: receiveQuantity)',
  `storage_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  `temperature` decimal(5, 2) NULL DEFAULT NULL COMMENT '????(???)',
  `humidity` decimal(5, 2) NULL DEFAULT NULL COMMENT '????(%)',
  `storage_condition` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??????',
  `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???/???(????: receiver)',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '??URL(JSON??)',
  `tx_hash` varchar(66) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???????',
  `block_number` bigint NULL DEFAULT NULL COMMENT '????',
  `chain_time` datetime NULL DEFAULT NULL COMMENT '????',
  `data_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '数据哈希(SHA-256)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??(????: remark, ??200?)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_batch_code`(`batch_code` ASC) USING BTREE,
  INDEX `idx_storage_enterprise_id`(`storage_enterprise_id` ASC) USING BTREE,
  INDEX `idx_storage_type`(`storage_type` ASC) USING BTREE,
  INDEX `idx_storage_date`(`storage_date` ASC) USING BTREE,
  INDEX `idx_tx_hash`(`tx_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_template
-- ----------------------------
DROP TABLE IF EXISTS `trace_template`;
CREATE TABLE `trace_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `template_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `template_type` tinyint NOT NULL COMMENT '????: 1-?? 2-???',
  `product_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '????(JSON??)',
  `sort` int NULL DEFAULT 0 COMMENT '??',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '??: 0-?? 1-??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_template_code`(`template_code` ASC) USING BTREE,
  INDEX `idx_template_type`(`template_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for trace_transport
-- ----------------------------
DROP TABLE IF EXISTS `trace_transport`;
CREATE TABLE `trace_transport`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `batch_id` bigint NOT NULL COMMENT '??ID',
  `batch_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??????',
  `logistics_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '鐗╂祦鍗曞彿(鍓嶇瀛楁: logisticsNo)',
  `transport_enterprise_id` bigint NULL DEFAULT NULL COMMENT '????ID',
  `transport_date` date NOT NULL COMMENT '????',
  `plate_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???',
  `driver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `driver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `receiver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '鏀朵欢浜哄鍚?鍓嶇瀛楁: receiverName)',
  `departure_location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???',
  `destination` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???',
  `departure_time` datetime NULL DEFAULT NULL COMMENT '????',
  `arrival_time` datetime NULL DEFAULT NULL COMMENT '????',
  `transport_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '????',
  `transport_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  `temperature` decimal(5, 2) NULL DEFAULT NULL COMMENT '????(???)',
  `humidity` decimal(5, 2) NULL DEFAULT NULL COMMENT '????(%)',
  `transport_condition` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `gps_track` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'GPS??(JSON??)',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '??URL(JSON??)',
  `tx_hash` varchar(66) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???????',
  `block_number` bigint NULL DEFAULT NULL COMMENT '????',
  `chain_time` datetime NULL DEFAULT NULL COMMENT '????',
  `data_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '数据哈希(SHA-256)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '????',
  `create_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '???ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '??',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_batch_code`(`batch_code` ASC) USING BTREE,
  INDEX `idx_transport_enterprise_id`(`transport_enterprise_id` ASC) USING BTREE,
  INDEX `idx_transport_date`(`transport_date` ASC) USING BTREE,
  INDEX `idx_tx_hash`(`tx_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_scan_log
-- ----------------------------
DROP TABLE IF EXISTS `user_scan_log`;
CREATE TABLE `user_scan_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `batch_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????',
  `batch_id` bigint NULL DEFAULT NULL COMMENT '??ID',
  `openid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????OpenID',
  `scan_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `scan_location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'IP??',
  `device_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '????',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_batch_code`(`batch_code` ASC) USING BTREE,
  INDEX `idx_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_scan_time`(`scan_time` ASC) USING BTREE,
  INDEX `idx_openid`(`openid` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????????' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
