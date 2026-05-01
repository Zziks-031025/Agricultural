-- =============================================
-- 农产品溯源系统最终部署数据库脚本
-- 生成日期: 2026-03-11
-- 说明:
--   1. 该脚本用于全新环境一次性创建数据库
--   2. 已移除 agricultural.sql 中与本项目无关的实验/数据集类表
--   3. 已保留当前前后端实际使用的权限字段与业务表结构
--   4. 已合并基础初始化数据，不包含测试业务数据与测试账号数据
-- =============================================

DROP DATABASE IF EXISTS `agricultural`;
CREATE DATABASE `agricultural` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `agricultural`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_type` TINYINT NOT NULL COMMENT '角色类型: 1-平台管理员 2-种植养殖企业 3-加工宰杀企业 4-检疫质检企业',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

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
  `cover_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '企业背景图/封面图',
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
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

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
  `receiver` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '接收人',
  `receive_date` date NULL DEFAULT NULL COMMENT '接收日期',
  `receive_enterprise_id` bigint NULL DEFAULT NULL COMMENT '接收企业ID（加工企业）',
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
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '????' ROW_FORMAT = Dynamic;

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
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '软删除标记 0-正常 1-已删除',
  `delete_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_batch_code`(`batch_code` ASC) USING BTREE,
  INDEX `idx_record_type`(`record_type` ASC) USING BTREE,
  INDEX `idx_record_date`(`record_date` ASC) USING BTREE,
  INDEX `idx_tx_hash`(`tx_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '??/?????' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

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
  `input_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '投入数量单位',
  `output_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '????(????: outputQuantity)',
  `output_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '产出数量单位',
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
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `trace_transport`;
CREATE TABLE `trace_transport`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `batch_id` bigint NOT NULL COMMENT '??ID',
  `batch_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '??????',
  `logistics_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '鐗╂祦鍗曞彿(鍓嶇瀛楁: logisticsNo)',
  `transport_enterprise_id` bigint NULL DEFAULT NULL COMMENT '????ID',
  `receive_enterprise_id` bigint NULL DEFAULT NULL COMMENT '目标接收企业ID（加工企业）',
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
  INDEX `idx_tx_hash`(`tx_hash` ASC) USING BTREE,
  INDEX `idx_receive_enterprise_id`(`receive_enterprise_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '????????' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

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

DROP TABLE IF EXISTS `sys_banner`;
CREATE TABLE `sys_banner`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enterprise_id` bigint NULL DEFAULT NULL COMMENT '所属企业ID(NULL表示平台级通用Banner)',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述文案',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片URL',
  `link_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '点击跳转链接(可选)',
  `target_type` tinyint NOT NULL DEFAULT 0 COMMENT '展示对象: 0-所有人(游客) 1-种植养殖企业 2-加工宰杀企业 3-检疫质检企业',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序(数值越小越靠前)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_enterprise_id`(`enterprise_id` ASC) USING BTREE,
  INDEX `idx_target_type`(`target_type` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '轮播图管理表' ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `edu_article`;
CREATE TABLE `edu_article`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enterprise_id` bigint NULL DEFAULT NULL COMMENT '所属企业ID(NULL表示平台级通用文章)',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类: farming-养殖技术 vaccine-防疫知识 process-加工规范 hygiene-卫生标准 quarantine-检疫规范 blockchain-区块链科普 safety-食品安全',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '作者',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '摘要',
  `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面图URL',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '正文内容(JSON格式)',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '阅读量',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序(数值越小越靠前)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-草稿 1-已发布',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_enterprise_id`(`enterprise_id` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '科普教育文章表' ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `enterprise_audit_image`;
CREATE TABLE `enterprise_audit_image`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enterprise_id` bigint NOT NULL COMMENT '企业ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '提交用户ID',
  `field_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字段名: avatar/business_license/production_license/logo',
  `old_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '旧图片URL',
  `new_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '新图片URL',
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '审核状态: 0-待审核 1-通过 2-拒绝',
  `audit_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注(拒绝原因)',
  `audit_by` bigint NULL DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_enterprise_id`(`enterprise_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_audit_status`(`audit_status` ASC) USING BTREE,
  INDEX `idx_field_name`(`field_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '企业图片审核表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???????' ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `sys_feedback`;
CREATE TABLE `sys_feedback`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '提交用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '反馈内容',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态: 0-待处理, 1-已处理',
  `reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '回复内容',
  `reply_time` datetime NULL DEFAULT NULL COMMENT '回复时间',
  `reply_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '回复人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_feedback_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_feedback_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户反馈表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

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
  `user_types` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '閫傜敤鐢ㄦ埛绫诲瀷(閫楀彿鍒嗛殧): 1-骞冲彴绠＄悊鍛?2-浼佷笟鐢ㄦ埛 3-鏅??鐢ㄦ埛',
  `enterprise_types` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '閫傜敤浼佷笟绫诲瀷(閫楀彿鍒嗛殧): 1-绉嶆?鍏绘畺 2-鍔犲伐瀹版潃 3-妫?柅璐ㄦ?',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_menu_code`(`menu_code` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1502 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '接收用户ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'system' COMMENT '消息类型: system-系统通知, business-业务提醒',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息标题',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '消息摘要',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '消息正文',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
  `action_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联跳转地址',
  `action_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作按钮标题',
  `action_desc` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作按钮描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_message_type`(`type` ASC) USING BTREE,
  INDEX `idx_message_read`(`is_read` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统消息通知表' ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '????ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '?????',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作模块',
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
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '?????' ROW_FORMAT = Dynamic;

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
  `enterprise_type` tinyint NULL DEFAULT NULL COMMENT '浼佷笟绫诲瀷: 1-绉嶆?鍏绘畺 2-鍔犲伐瀹版潃 3-妫?柅璐ㄦ? (浠巈nterprise_info鍚屾?)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  INDEX `idx_enterprise_id`(`enterprise_id` ASC) USING BTREE,
  INDEX `idx_user_type`(`user_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '???' ROW_FORMAT = Dynamic;

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

-- 9.1 初始化管理员账号
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `user_type`, `status`, `create_time`) 
VALUES (1, 'admin', 'admin123', '系统管理员', 1, 1, NOW());
-- 默认密码: admin123

-- 9.2 初始化角色
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `role_type`, `sort`, `status`, `create_time`) VALUES
(1, 'PLATFORM_ADMIN', '平台管理员', 1, 1, 1, NOW()),
(2, 'PLANTING_BREEDING', '种植养殖企业', 2, 2, 1, NOW()),
(3, 'PROCESSING', '加工宰杀企业', 3, 3, 1, NOW()),
(4, 'INSPECTION', '检疫质检企业', 4, 4, 1, NOW());

-- 9.3 初始化用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_time`) VALUES (1, 1, NOW());

-- 9.4 初始化菜单(平台管理端)
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`) VALUES
-- 企业管理模块
(100, 0, '企业管理', 'enterprise', 1, '/enterprise', NULL, NULL, 'enterprise', 1, 1, 1),
(101, 100, '企业入驻审核', 'enterprise:audit', 2, '/enterprise/audit', 'enterprise/audit/index', 'enterprise:audit:list', 'audit', 1, 1, 1),
(102, 100, '企业资质审核', 'enterprise:qualification', 2, '/enterprise/qualification', 'enterprise/qualification/index', 'enterprise:qualification:list', 'qualification', 2, 1, 1),
(103, 100, '企业信息管理', 'enterprise:info', 2, '/enterprise/info', 'enterprise/info/index', 'enterprise:info:list', 'info', 3, 1, 1),
(104, 100, '企业账号管理', 'enterprise:account', 2, '/enterprise/account', 'enterprise/account/index', 'enterprise:account:list', 'account', 4, 1, 1),
(105, 100, '企业分类管理', 'enterprise:category', 2, '/enterprise/category', 'enterprise/category/index', 'enterprise:category:list', 'category', 5, 1, 1),
-- 系统管理模块
(200, 0, '系统管理', 'system', 1, '/system', NULL, NULL, 'system', 2, 1, 1),
(201, 200, '用户管理', 'system:user', 2, '/system/user', 'system/user/index', 'system:user:list', 'user', 1, 1, 1),
(202, 200, '角色权限管理', 'system:role', 2, '/system/role', 'system/role/index', 'system:role:list', 'role', 2, 1, 1),
(203, 200, '菜单管理', 'system:menu', 2, '/system/menu', 'system/menu/index', 'system:menu:list', 'menu', 3, 1, 1),
(204, 200, '系统参数配置', 'system:config', 2, '/system/config', 'system/config/index', 'system:config:list', 'config', 4, 1, 1),
(205, 200, '操作日志', 'system:log', 2, '/system/log', 'system/log/index', 'system:log:list', 'log', 5, 1, 1),
-- 模版管理模块
(300, 0, '模版管理', 'template', 1, '/template', NULL, NULL, 'template', 3, 1, 1),
(301, 300, '种植模版配置', 'template:planting', 2, '/template/planting', 'template/planting/index', 'template:planting:list', 'planting', 1, 1, 1),
(302, 300, '养殖模版配置', 'template:breeding', 2, '/template/breeding', 'template/breeding/index', 'template:breeding:list', 'breeding', 2, 1, 1),
(303, 300, '溯源环节定义', 'template:stage', 2, '/template/stage', 'template/stage/index', 'template:stage:list', 'stage', 3, 1, 1),
(304, 300, '数据字段配置', 'template:field', 2, '/template/field', 'template/field/index', 'template:field:list', 'field', 4, 1, 1),
-- 区块链管理模块
(400, 0, '区块链管理', 'blockchain', 1, '/blockchain', NULL, NULL, 'blockchain', 4, 1, 1),
(401, 400, '上链数据统计', 'blockchain:data', 2, '/blockchain/data', 'blockchain/data/index', 'blockchain:data:list', 'data', 1, 1, 1),
(402, 400, 'Gas费用管理', 'blockchain:gas', 2, '/blockchain/gas', 'blockchain/gas/index', 'blockchain:gas:list', 'gas', 2, 1, 1),
(403, 400, '区块链节点监控', 'blockchain:node', 2, '/blockchain/node', 'blockchain/node/index', 'blockchain:node:list', 'node', 3, 1, 1),
(404, 400, '智能合约管理', 'blockchain:contract', 2, '/blockchain/contract', 'blockchain/contract/index', 'blockchain:contract:list', 'contract', 4, 1, 1),
-- 数据监管模块
(500, 0, '数据监管', 'supervision', 1, '/supervision', NULL, NULL, 'supervision', 5, 1, 1),
(501, 500, '溯源数据查询', 'supervision:trace', 2, '/supervision/trace', 'supervision/trace/index', 'supervision:trace:list', 'trace', 1, 1, 1),
(502, 500, '数据完整性检查', 'supervision:integrity', 2, '/supervision/integrity', 'supervision/integrity/index', 'supervision:integrity:list', 'integrity', 2, 1, 1),
(503, 500, '异常数据预警', 'supervision:warning', 2, '/supervision/warning', 'supervision/warning/index', 'supervision:warning:list', 'warning', 3, 1, 1),
(504, 500, '数据审计', 'supervision:audit', 2, '/supervision/audit', 'supervision/audit/index', 'supervision:audit:list', 'audit', 4, 1, 1),
-- 统计分析模块
(600, 0, '统计分析', 'statistics', 1, '/statistics', NULL, NULL, 'statistics', 6, 1, 1),
(601, 600, '平台数据统计大屏', 'statistics:dashboard', 2, '/statistics/dashboard', 'statistics/dashboard/index', 'statistics:dashboard:view', 'dashboard', 1, 1, 1),
(602, 600, '企业数据统计', 'statistics:enterprise', 2, '/statistics/enterprise', 'statistics/enterprise/index', 'statistics:enterprise:list', 'enterprise', 2, 1, 1),
(603, 600, '溯源查询统计', 'statistics:scan', 2, '/statistics/scan', 'statistics/scan/index', 'statistics:scan:list', 'scan', 3, 1, 1),
(604, 600, '区块链数据统计', 'statistics:blockchain', 2, '/statistics/blockchain', 'statistics/blockchain/index', 'statistics:blockchain:list', 'blockchain', 4, 1, 1);

-- 9.5 初始化菜单(企业管理端)
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`) VALUES
(1000, 0, '企业信息管理', 'enterprise:self', 1, '/enterprise/self', NULL, NULL, 'enterprise', 1, 1, 1),
(1001, 1000, '企业信息', 'enterprise:self:info', 2, '/enterprise/self/info', 'enterprise/self/info', 'enterprise:self:view', 'info', 1, 1, 1),
(1100, 0, '生产管理', 'production', 1, '/production', NULL, NULL, 'production', 2, 1, 1),
(1101, 1100, '种植/养殖初始化', 'production:init', 2, '/production/init', 'production/init/index', 'production:init:add', 'init', 1, 1, 1),
(1102, 1100, '过程信息记录', 'production:process', 2, '/production/process', 'production/process/index', 'production:process:add', 'process', 2, 1, 1),
(1200, 0, '加工与来源', 'processing', 1, '/processing', NULL, NULL, 'processing', 3, 1, 1),
(1201, 1200, '原料/加工记录', 'processing:material', 2, '/processing/material', 'processing/material/index', 'processing:material:add', 'material', 1, 1, 1),
(1202, 1200, '产品加工记录', 'processing:product', 2, '/processing/product', 'processing/product/index', 'processing:product:add', 'product', 2, 1, 1),
(1300, 0, '质检与流通', 'circulation', 1, '/circulation', NULL, NULL, 'circulation', 4, 1, 1),
(1301, 1300, '质量检测/检疫申报', 'circulation:inspection', 2, '/circulation/inspection', 'circulation/inspection/index', 'circulation:inspection:add', 'inspection', 1, 1, 1),
(1302, 1300, '仓储信息记录', 'circulation:storage', 2, '/circulation/storage', 'circulation/storage/index', 'circulation:storage:add', 'storage', 2, 1, 1),
(1303, 1300, '运输信息记录', 'circulation:transport', 2, '/circulation/transport', 'circulation/transport/index', 'circulation:transport:add', 'transport', 3, 1, 1),
(1304, 1300, '销售信息记录', 'circulation:sale', 2, '/circulation/sale', 'circulation/sale/index', 'circulation:sale:add', 'sale', 4, 1, 1),
(1400, 0, '区块链', 'chain', 1, '/chain', NULL, NULL, 'chain', 5, 1, 1),
(1401, 1400, '区块链上链', 'chain:upload', 2, '/chain/upload', 'chain/upload/index', 'chain:upload:add', 'upload', 1, 1, 1),
(1500, 0, '数据统计分析', 'analysis', 1, '/analysis', NULL, NULL, 'analysis', 6, 1, 1),
(1501, 1500, '数据统计', 'analysis:statistics', 2, '/analysis/statistics', 'analysis/statistics/index', 'analysis:statistics:view', 'statistics', 1, 1, 1);

-- 9.6 初始化角色菜单关联(平台管理员拥有所有平台管理端菜单)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 1, id FROM `sys_menu` WHERE id BETWEEN 100 AND 699;

-- 9.7 初始化角色菜单关联(种植养殖企业)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 1000), (2, 1001),
(2, 1100), (2, 1101), (2, 1102),
(2, 1300), (2, 1302), (2, 1303), (2, 1304),
(2, 1400), (2, 1401),
(2, 1500), (2, 1501);

-- 9.8 初始化角色菜单关联(加工宰杀企业)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(3, 1000), (3, 1001),
(3, 1200), (3, 1201), (3, 1202),
(3, 1300), (3, 1302), (3, 1303), (3, 1304),
(3, 1400), (3, 1401),
(3, 1500), (3, 1501);

-- 9.9 初始化角色菜单关联(检疫质检企业)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(4, 1000), (4, 1001),
(4, 1300), (4, 1301), (4, 1302), (4, 1303), (4, 1304),
(4, 1400), (4, 1401),
(4, 1500), (4, 1501);

-- 9.10 初始化字典类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`) VALUES
('用户类型', 'sys_user_type', 1),
('企业类型', 'enterprise_type', 1),
('审核状态', 'audit_status', 1),
('产品类型', 'product_type', 1),
('批次状态', 'batch_status', 1),
('记录类型-肉鸡', 'record_type_chicken', 1),
('记录类型-西红柿', 'record_type_tomato', 1),
('加工方式', 'process_method', 1),
('检疫结果', 'check_result', 1),
('仓储类型', 'storage_type', 1),
('业务类型', 'business_type', 1),
('交易状态', 'transaction_status', 1);

-- 9.11 初始化字典数据
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`) VALUES
-- 用户类型
('sys_user_type', '平台管理员', '1', 1, 1),
('sys_user_type', '企业用户', '2', 2, 1),
('sys_user_type', '普通用户', '3', 3, 1),

-- 企业类型
('enterprise_type', '种植养殖', '1', 1, 1),
('enterprise_type', '加工宰杀', '2', 2, 1),
('enterprise_type', '检疫质检', '3', 3, 1),

-- 审核状态
('audit_status', '待审核', '0', 1, 1),
('audit_status', '审核通过', '1', 2, 1),
('audit_status', '审核拒绝', '2', 3, 1),

-- 产品类型(与前端 templateType 对应)
('product_type', '肉鸡', '1', 1, 1),
('product_type', '西红柿', '2', 2, 1),

-- 批次状态
('batch_status', '初始化', '1', 1, 1),
('batch_status', '生长中', '2', 2, 1),
('batch_status', '已收获', '3', 3, 1),
('batch_status', '加工中', '4', 4, 1),
('batch_status', '已检疫', '5', 5, 1),
('batch_status', '已入库', '6', 6, 1),
('batch_status', '运输中', '7', 7, 1),
('batch_status', '已销售', '8', 8, 1),

-- 记录类型 - 肉鸡(与前端 recordType 对应)
('record_type_chicken', '喂养', 'feeding', 1, 1),
('record_type_chicken', '防疫', 'vaccine', 2, 1),
('record_type_chicken', '巡查', 'inspect', 3, 1),

-- 记录类型 - 西红柿(与前端 recordType 对应)
('record_type_tomato', '施肥', 'fertilize', 1, 1),
('record_type_tomato', '浇水', 'irrigate', 2, 1),
('record_type_tomato', '用药', 'pesticide', 3, 1),

-- 加工方式(与前端 processingMethod 对应)
('process_method', '整鸡', '整鸡', 1, 1),
('process_method', '分切', '分切', 2, 1),
('process_method', '去骨', '去骨', 3, 1),

-- 检疫结果(与前端 result 对应: 1-合格 0-不合格)
('check_result', '合格', '1', 1, 1),
('check_result', '不合格', '0', 2, 1),

-- 仓储类型
('storage_type', '入库', '1', 1, 1),
('storage_type', '出库', '2', 2, 1),
('storage_type', '库存盘点', '3', 3, 1),

-- 业务类型
('business_type', '批次', '1', 1, 1),
('business_type', '生长记录', '2', 2, 1),
('business_type', '加工', '3', 3, 1),
('business_type', '检疫', '4', 4, 1),
('business_type', '仓储', '5', 5, 1),
('business_type', '运输', '6', 6, 1),
('business_type', '销售', '7', 7, 1),

-- 交易状态
('transaction_status', '待确认', '0', 1, 1),
('transaction_status', '成功', '1', 2, 1),
('transaction_status', '失败', '2', 3, 1);

-- 9.12 初始化系统配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('blockchain.network', 'testnet', 'blockchain', '区块链网络类型'),
('blockchain.rpc.url', 'https://sepolia.infura.io/v3/YOUR_PROJECT_ID', 'blockchain', '区块链RPC节点地址'),
('blockchain.contract.address', '', 'blockchain', '智能合约地址'),
('blockchain.private.key', '', 'blockchain', '私钥(加密存储)'),
('blockchain.gas.limit', '300000', 'blockchain', 'Gas限制'),
('blockchain.gas.price', '20', 'blockchain', 'Gas价格(Gwei)'),
('qrcode.base.url', 'https://your-domain.com/trace/', 'system', '溯源二维码基础URL'),
('file.upload.path', '/uploads/', 'system', '文件上传路径'),
('file.upload.max.size', '10485760', 'system', '文件上传最大大小(字节)');

-- 9.13 同步菜单适用范围（直接采用当前运行代码所需字段）
UPDATE `sys_menu` SET user_types='1', enterprise_types=NULL
WHERE id BETWEEN 100 AND 699;

UPDATE `sys_menu` SET user_types='2', enterprise_types='1,2,3'
WHERE id BETWEEN 1000 AND 1099;

UPDATE `sys_menu` SET user_types='2', enterprise_types='1'
WHERE id BETWEEN 1100 AND 1199;

UPDATE `sys_menu` SET user_types='2', enterprise_types='2'
WHERE id BETWEEN 1200 AND 1299;

UPDATE `sys_menu` SET user_types='2', enterprise_types='1,2'
WHERE id IN (1300, 1301, 1302, 1303, 1304);

UPDATE `sys_menu` SET user_types='2', enterprise_types='1,2,3'
WHERE id BETWEEN 1400 AND 1499;

UPDATE `sys_menu` SET user_types='2', enterprise_types='1,2,3'
WHERE id BETWEEN 1500 AND 1599;

-- 9.14 初始化演示企业数据（对应 DataSeeder 中使用的 enterpriseId 1/2/3）
INSERT INTO `enterprise_info` (`id`, `enterprise_code`, `enterprise_name`, `enterprise_type`, `legal_person`, `contact_phone`, `contact_email`, `province`, `city`, `district`, `address`, `audit_status`, `create_time`, `update_time`) VALUES
(1, 'ENT001', '河北绿源养殖有限公司', 1, '张三', '13800000001', 'farm@example.com', '河北省', '保定市', '满城区', '河北省保定市满城区养殖基地', 1, NOW(), NOW()),
(2, 'ENT002', '北京顺鑫食品加工有限公司', 2, '李四', '13800000002', 'process@example.com', '北京市', '顺义区', '顺义区', '北京市顺义区加工产业园', 1, NOW(), NOW()),
(3, 'ENT003', '北京市动物卫生监督所', 3, '王五', '13800000003', 'inspect@example.com', '北京市', '朝阳区', '朝阳区', '北京市朝阳区检疫中心', 1, NOW(), NOW());

-- 9.15 初始化演示企业用户（密码均为 admin123）
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `user_type`, `enterprise_id`, `enterprise_type`, `status`, `create_time`) VALUES
(2, 'farmer', 'farmer123', '养殖企业用户', 2, 1, 1, 1, NOW()),
(3, 'processor', 'processor123', '加工企业用户', 2, 2, 2, 1, NOW()),
(4, 'inspector', 'inspector123', '检疫企业用户', 2, 3, 3, 1, NOW());
-- 默认密码: admin123

-- 9.16 初始化企业用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_time`) VALUES
(2, 2, NOW()),
(3, 3, NOW()),
(4, 4, NOW());

-- 9.17 同步用户企业类型（兼容当前后端实体字段）
UPDATE `sys_user` u
INNER JOIN `enterprise_info` e ON u.enterprise_id = e.id
SET u.enterprise_type = e.enterprise_type
WHERE u.user_type = 2;
SET FOREIGN_KEY_CHECKS = 1;
