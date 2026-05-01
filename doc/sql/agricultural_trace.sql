-- =============================================
-- 基于区块链的农产品溯源模版系统 - 数据库设计
-- MySQL 8.0
-- 创建日期: 2026-01-30
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 1. RBAC 权限管理模块
-- =============================================

-- 1.1 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '密码（加密）',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `user_type` TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型：1-平台管理员 2-企业用户 3-普通用户',
  `enterprise_id` BIGINT DEFAULT NULL COMMENT '关联企业ID（企业用户必填）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 1.2 角色表
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_type` TINYINT NOT NULL COMMENT '角色类型：1-平台管理员 2-种植养殖企业 3-加工宰杀企业 4-检疫质检企业',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 1.3 菜单权限表
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID（0表示顶级菜单）',
  `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `menu_code` VARCHAR(50) NOT NULL COMMENT '菜单编码',
  `menu_type` TINYINT NOT NULL COMMENT '菜单类型：1-目录 2-菜单 3-按钮',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
  `component` VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
  `perms` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见：0-隐藏 1-显示',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_code` (`menu_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

-- 1.4 用户角色关联表
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

-- 1.5 角色菜单关联表
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

-- =============================================
-- 2. 企业管理模块
-- =============================================

-- 2.1 企业信息表
DROP TABLE IF EXISTS `enterprise_info`;
CREATE TABLE `enterprise_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '企业ID',
  `enterprise_code` VARCHAR(50) NOT NULL COMMENT '企业编码（统一社会信用代码）',
  `enterprise_name` VARCHAR(100) NOT NULL COMMENT '企业名称',
  `enterprise_type` TINYINT NOT NULL COMMENT '企业类型：1-种植养殖 2-加工宰杀 3-检疫质检',
  `legal_person` VARCHAR(50) DEFAULT NULL COMMENT '法人代表',
  `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `contact_email` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
  `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `district` VARCHAR(50) DEFAULT NULL COMMENT '区县',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  `business_license` VARCHAR(255) DEFAULT NULL COMMENT '营业执照URL',
  `production_license` VARCHAR(255) DEFAULT NULL COMMENT '生产许可证URL',
  `other_certificates` TEXT DEFAULT NULL COMMENT '其他资质证书（JSON格式）',
  `introduction` TEXT DEFAULT NULL COMMENT '企业简介',
  `logo` VARCHAR(255) DEFAULT NULL COMMENT '企业LOGO',
  `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核 1-审核通过 2-审核拒绝',
  `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_by` BIGINT DEFAULT NULL COMMENT '审核人',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enterprise_code` (`enterprise_code`),
  KEY `idx_enterprise_type` (`enterprise_type`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业信息表';

-- =============================================
-- 3. 溯源模版管理模块
-- =============================================

-- 3.1 溯源模版表
DROP TABLE IF EXISTS `trace_template`;
CREATE TABLE `trace_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模版ID',
  `template_code` VARCHAR(50) NOT NULL COMMENT '模版编码',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模版名称',
  `template_type` TINYINT NOT NULL COMMENT '模版类型：1-种植 2-养殖',
  `product_category` VARCHAR(50) DEFAULT NULL COMMENT '产品类别（如：西红柿、肉鸡）',
  `description` TEXT DEFAULT NULL COMMENT '模版描述',
  `config_json` TEXT DEFAULT NULL COMMENT '模版配置（JSON格式）',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_template_type` (`template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='溯源模版表';

-- 3.2 溯源环节定义表
DROP TABLE IF EXISTS `trace_stage`;
CREATE TABLE `trace_stage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '环节ID',
  `template_id` BIGINT NOT NULL COMMENT '所属模版ID',
  `stage_code` VARCHAR(50) NOT NULL COMMENT '环节编码',
  `stage_name` VARCHAR(100) NOT NULL COMMENT '环节名称',
  `stage_type` TINYINT NOT NULL COMMENT '环节类型：1-初始化 2-生长过程 3-加工 4-检疫 5-仓储 6-运输 7-销售',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `is_required` TINYINT NOT NULL DEFAULT 1 COMMENT '是否必填：0-否 1-是',
  `description` TEXT DEFAULT NULL COMMENT '环节描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_stage_type` (`stage_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='溯源环节定义表';

-- 3.3 溯源字段配置表
DROP TABLE IF EXISTS `trace_field`;
CREATE TABLE `trace_field` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '字段ID',
  `stage_id` BIGINT NOT NULL COMMENT '所属环节ID',
  `field_code` VARCHAR(50) NOT NULL COMMENT '字段编码',
  `field_name` VARCHAR(100) NOT NULL COMMENT '字段名称',
  `field_type` VARCHAR(20) NOT NULL COMMENT '字段类型：text/number/date/select/image/video',
  `field_options` TEXT DEFAULT NULL COMMENT '字段选项（JSON格式，用于select类型）',
  `is_required` TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填：0-否 1-是',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `placeholder` VARCHAR(200) DEFAULT NULL COMMENT '输入提示',
  `default_value` VARCHAR(200) DEFAULT NULL COMMENT '默认值',
  `validation_rule` VARCHAR(200) DEFAULT NULL COMMENT '校验规则',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_stage_id` (`stage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='溯源字段配置表';

-- =============================================
-- 4. 溯源核心业务模块
-- =============================================

-- 4.1 批次表（核心主表）
DROP TABLE IF EXISTS `trace_batch`;
CREATE TABLE `trace_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `batch_code` VARCHAR(50) NOT NULL COMMENT '批次编号（唯一溯源码）',
  `template_id` BIGINT NOT NULL COMMENT '使用的模版ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '产品名称',
  `product_type` TINYINT NOT NULL COMMENT '产品类型：1-种植 2-养殖',
  `enterprise_id` BIGINT NOT NULL COMMENT '创建企业ID',
  `qr_code_url` VARCHAR(255) DEFAULT NULL COMMENT '溯源二维码URL',
  `init_quantity` DECIMAL(10,2) DEFAULT NULL COMMENT '初始数量',
  `current_quantity` DECIMAL(10,2) DEFAULT NULL COMMENT '当前数量',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `production_date` DATE DEFAULT NULL COMMENT '生产日期',
  `expected_harvest_date` DATE DEFAULT NULL COMMENT '预计收获/出栏日期',
  `actual_harvest_date` DATE DEFAULT NULL COMMENT '实际收获/出栏日期',
  `batch_status` TINYINT NOT NULL DEFAULT 1 COMMENT '批次状态：1-初始化 2-生长中 3-已收获 4-加工中 5-已检疫 6-已入库 7-运输中 8-已销售',
  `tx_hash` VARCHAR(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `block_number` BIGINT DEFAULT NULL COMMENT '区块高度',
  `chain_time` DATETIME DEFAULT NULL COMMENT '上链时间',
  `data_hash` VARCHAR(64) DEFAULT NULL COMMENT '数据哈希值（用于验证）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_code` (`batch_code`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_batch_status` (`batch_status`),
  KEY `idx_tx_hash` (`tx_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批次表';

-- 4.2 种植养殖过程记录表
DROP TABLE IF EXISTS `trace_process`;
CREATE TABLE `trace_process` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `record_type` TINYINT NOT NULL COMMENT '记录类型：1-播种/投苗 2-施肥 3-浇水 4-用药 5-环境监测 6-日常巡查 7-其他',
  `operator` VARCHAR(50) DEFAULT NULL COMMENT '操作人',
  `operation_content` TEXT DEFAULT NULL COMMENT '操作内容',
  `environment_data` TEXT DEFAULT NULL COMMENT '环境数据（JSON格式：温度、湿度等）',
  `fertilizer_name` VARCHAR(100) DEFAULT NULL COMMENT '肥料名称',
  `fertilizer_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '肥料用量',
  `pesticide_name` VARCHAR(100) DEFAULT NULL COMMENT '农药/兽药名称',
  `pesticide_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '农药/兽药用量',
  `images` TEXT DEFAULT NULL COMMENT '图片URL（JSON数组）',
  `videos` TEXT DEFAULT NULL COMMENT '视频URL（JSON数组）',
  `tx_hash` VARCHAR(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `block_number` BIGINT DEFAULT NULL COMMENT '区块高度',
  `chain_time` DATETIME DEFAULT NULL COMMENT '上链时间',
  `data_hash` VARCHAR(64) DEFAULT NULL COMMENT '数据哈希值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_record_date` (`record_date`),
  KEY `idx_record_type` (`record_type`),
  KEY `idx_tx_hash` (`tx_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='种植养殖过程记录表';

-- 4.3 加工宰杀记录表
DROP TABLE IF EXISTS `trace_processing`;
CREATE TABLE `trace_processing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID',
  `source_batch_id` BIGINT DEFAULT NULL COMMENT '来源批次ID（原料批次）',
  `processing_enterprise_id` BIGINT NOT NULL COMMENT '加工企业ID',
  `processing_date` DATE NOT NULL COMMENT '加工日期',
  `processing_type` TINYINT NOT NULL COMMENT '加工类型：1-宰杀 2-分割 3-包装 4-深加工',
  `input_quantity` DECIMAL(10,2) DEFAULT NULL COMMENT '投入数量',
  `output_quantity` DECIMAL(10,2) DEFAULT NULL COMMENT '产出数量',
  `processing_method` VARCHAR(200) DEFAULT NULL COMMENT '加工方式',
  `processing_equipment` VARCHAR(200) DEFAULT NULL COMMENT '加工设备',
  `operator` VARCHAR(50) DEFAULT NULL COMMENT '操作人',
  `workshop_info` VARCHAR(200) DEFAULT NULL COMMENT '车间信息',
  `temperature` DECIMAL(5,2) DEFAULT NULL COMMENT '加工温度',
  `humidity` DECIMAL(5,2) DEFAULT NULL COMMENT '加工湿度',
  `images` TEXT DEFAULT NULL COMMENT '图片URL（JSON数组）',
  `videos` TEXT DEFAULT NULL COMMENT '视频URL（JSON数组）',
  `tx_hash` VARCHAR(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `block_number` BIGINT DEFAULT NULL COMMENT '区块高度',
  `chain_time` DATETIME DEFAULT NULL COMMENT '上链时间',
  `data_hash` VARCHAR(64) DEFAULT NULL COMMENT '数据哈希值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_source_batch_id` (`source_batch_id`),
  KEY `idx_processing_enterprise_id` (`processing_enterprise_id`),
  KEY `idx_processing_date` (`processing_date`),
  KEY `idx_tx_hash` (`tx_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加工宰杀记录表';

-- 4.4 检疫质检记录表
DROP TABLE IF EXISTS `trace_inspection`;
CREATE TABLE `trace_inspection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID',
  `inspection_enterprise_id` BIGINT NOT NULL COMMENT '检疫机构ID',
  `inspection_date` DATE NOT NULL COMMENT '检疫日期',
  `inspection_type` TINYINT NOT NULL COMMENT '检疫类型：1-产地检疫 2-屠宰检疫 3-运输检疫 4-市场检疫 5-质量检测',
  `inspector` VARCHAR(50) DEFAULT NULL COMMENT '检疫员',
  `inspection_certificate_no` VARCHAR(100) DEFAULT NULL COMMENT '检疫证书编号',
  `inspection_result` TINYINT NOT NULL COMMENT '检疫结果：1-合格 2-不合格',
  `inspection_items` TEXT DEFAULT NULL COMMENT '检测项目（JSON格式）',
  `inspection_data` TEXT DEFAULT NULL COMMENT '检测数据（JSON格式）',
  `unqualified_reason` VARCHAR(500) DEFAULT NULL COMMENT '不合格原因',
  `handle_method` VARCHAR(500) DEFAULT NULL COMMENT '处理方式',
  `certificate_url` VARCHAR(255) DEFAULT NULL COMMENT '检疫证书URL',
  `images` TEXT DEFAULT NULL COMMENT '图片URL（JSON数组）',
  `tx_hash` VARCHAR(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `block_number` BIGINT DEFAULT NULL COMMENT '区块高度',
  `chain_time` DATETIME DEFAULT NULL COMMENT '上链时间',
  `data_hash` VARCHAR(64) DEFAULT NULL COMMENT '数据哈希值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_inspection_enterprise_id` (`inspection_enterprise_id`),
  KEY `idx_inspection_date` (`inspection_date`),
  KEY `idx_inspection_result` (`inspection_result`),
  KEY `idx_tx_hash` (`tx_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检疫质检记录表';

-- 4.5 仓储记录表
DROP TABLE IF EXISTS `trace_storage`;
CREATE TABLE `trace_storage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID',
  `storage_enterprise_id` BIGINT NOT NULL COMMENT '仓储企业ID',
  `storage_type` TINYINT NOT NULL COMMENT '仓储类型：1-入库 2-出库 3-库存盘点',
  `storage_date` DATE NOT NULL COMMENT '仓储日期',
  `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '仓库名称',
  `warehouse_location` VARCHAR(200) DEFAULT NULL COMMENT '仓库位置',
  `storage_quantity` DECIMAL(10,2) DEFAULT NULL COMMENT '仓储数量',
  `storage_unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `temperature` DECIMAL(5,2) DEFAULT NULL COMMENT '仓储温度',
  `humidity` DECIMAL(5,2) DEFAULT NULL COMMENT '仓储湿度',
  `storage_condition` VARCHAR(200) DEFAULT NULL COMMENT '仓储条件',
  `operator` VARCHAR(50) DEFAULT NULL COMMENT '操作人',
  `images` TEXT DEFAULT NULL COMMENT '图片URL（JSON数组）',
  `tx_hash` VARCHAR(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `block_number` BIGINT DEFAULT NULL COMMENT '区块高度',
  `chain_time` DATETIME DEFAULT NULL COMMENT '上链时间',
  `data_hash` VARCHAR(64) DEFAULT NULL COMMENT '数据哈希值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_storage_enterprise_id` (`storage_enterprise_id`),
  KEY `idx_storage_date` (`storage_date`),
  KEY `idx_tx_hash` (`tx_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓储记录表';

-- 4.6 运输记录表
DROP TABLE IF EXISTS `trace_transport`;
CREATE TABLE `trace_transport` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID',
  `transport_enterprise_id` BIGINT DEFAULT NULL COMMENT '运输企业ID',
  `transport_date` DATE NOT NULL COMMENT '运输日期',
  `vehicle_no` VARCHAR(50) DEFAULT NULL COMMENT '车辆号码',
  `driver_name` VARCHAR(50) DEFAULT NULL COMMENT '司机姓名',
  `driver_phone` VARCHAR(20) DEFAULT NULL COMMENT '司机电话',
  `departure_location` VARCHAR(200) DEFAULT NULL COMMENT '出发地',
  `arrival_location` VARCHAR(200) DEFAULT NULL COMMENT '目的地',
  `departure_time` DATETIME DEFAULT NULL COMMENT '出发时间',
  `arrival_time` DATETIME DEFAULT NULL COMMENT '到达时间',
  `transport_quantity` DECIMAL(10,2) DEFAULT NULL COMMENT '运输数量',
  `transport_unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `transport_temperature` DECIMAL(5,2) DEFAULT NULL COMMENT '运输温度',
  `transport_condition` VARCHAR(200) DEFAULT NULL COMMENT '运输条件',
  `gps_track` TEXT DEFAULT NULL COMMENT 'GPS轨迹（JSON格式）',
  `images` TEXT DEFAULT NULL COMMENT '图片URL（JSON数组）',
  `tx_hash` VARCHAR(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `block_number` BIGINT DEFAULT NULL COMMENT '区块高度',
  `chain_time` DATETIME DEFAULT NULL COMMENT '上链时间',
  `data_hash` VARCHAR(64) DEFAULT NULL COMMENT '数据哈希值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_transport_enterprise_id` (`transport_enterprise_id`),
  KEY `idx_transport_date` (`transport_date`),
  KEY `idx_tx_hash` (`tx_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运输记录表';

-- 4.7 销售记录表
DROP TABLE IF EXISTS `trace_sale`;
CREATE TABLE `trace_sale` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID',
  `sale_enterprise_id` BIGINT NOT NULL COMMENT '销售企业ID',
  `sale_date` DATE NOT NULL COMMENT '销售日期',
  `buyer_name` VARCHAR(100) DEFAULT NULL COMMENT '购买方名称',
  `buyer_contact` VARCHAR(50) DEFAULT NULL COMMENT '购买方联系人',
  `buyer_phone` VARCHAR(20) DEFAULT NULL COMMENT '购买方电话',
  `sale_quantity` DECIMAL(10,2) DEFAULT NULL COMMENT '销售数量',
  `sale_unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `sale_price` DECIMAL(10,2) DEFAULT NULL COMMENT '销售单价',
  `total_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '销售总额',
  `sale_channel` VARCHAR(100) DEFAULT NULL COMMENT '销售渠道',
  `destination` VARCHAR(200) DEFAULT NULL COMMENT '销售目的地',
  `images` TEXT DEFAULT NULL COMMENT '图片URL（JSON数组）',
  `tx_hash` VARCHAR(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `block_number` BIGINT DEFAULT NULL COMMENT '区块高度',
  `chain_time` DATETIME DEFAULT NULL COMMENT '上链时间',
  `data_hash` VARCHAR(64) DEFAULT NULL COMMENT '数据哈希值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_sale_enterprise_id` (`sale_enterprise_id`),
  KEY `idx_sale_date` (`sale_date`),
  KEY `idx_tx_hash` (`tx_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售记录表';

-- =============================================
-- 5. 区块链管理模块
-- =============================================

-- 5.1 区块链交易记录表
DROP TABLE IF EXISTS `blockchain_transaction`;
CREATE TABLE `blockchain_transaction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `tx_hash` VARCHAR(66) NOT NULL COMMENT '交易哈希',
  `message_id` VARCHAR(100) NOT NULL COMMENT '消息ID（用于链上查询）',
  `business_type` TINYINT NOT NULL COMMENT '业务类型：1-批次 2-过程 3-加工 4-检疫 5-仓储 6-运输 7-销售',
  `business_id` BIGINT NOT NULL COMMENT '业务数据ID',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID',
  `data_hash` VARCHAR(64) NOT NULL COMMENT '数据哈希值',
  `contract_address` VARCHAR(42) NOT NULL COMMENT '合约地址',
  `from_address` VARCHAR(42) DEFAULT NULL COMMENT '发送地址',
  `block_number` BIGINT DEFAULT NULL COMMENT '区块高度',
  `block_hash` VARCHAR(66) DEFAULT NULL COMMENT '区块哈希',
  `gas_used` BIGINT DEFAULT NULL COMMENT '消耗Gas',
  `gas_price` BIGINT DEFAULT NULL COMMENT 'Gas价格（Wei）',
  `transaction_fee` DECIMAL(20,8) DEFAULT NULL COMMENT '交易费用（ETH）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待确认 1-成功 2-失败',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `chain_time` DATETIME DEFAULT NULL COMMENT '上链时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tx_hash` (`tx_hash`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_business_type_id` (`business_type`, `business_id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_block_number` (`block_number`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区块链交易记录表';

-- 5.2 智能合约管理表
DROP TABLE IF EXISTS `blockchain_contract`;
CREATE TABLE `blockchain_contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '合约ID',
  `contract_name` VARCHAR(100) NOT NULL COMMENT '合约名称',
  `contract_address` VARCHAR(42) NOT NULL COMMENT '合约地址',
  `contract_abi` TEXT DEFAULT NULL COMMENT '合约ABI',
  `contract_code` TEXT DEFAULT NULL COMMENT '合约源码',
  `deploy_tx_hash` VARCHAR(66) DEFAULT NULL COMMENT '部署交易哈希',
  `deploy_block_number` BIGINT DEFAULT NULL COMMENT '部署区块高度',
  `network` VARCHAR(50) DEFAULT NULL COMMENT '网络（mainnet/testnet）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_address` (`contract_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能合约管理表';

-- 5.3 Gas费用统计表
DROP TABLE IF EXISTS `blockchain_gas_fee`;
CREATE TABLE `blockchain_gas_fee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `enterprise_id` BIGINT DEFAULT NULL COMMENT '企业ID（NULL表示平台总计）',
  `transaction_count` INT DEFAULT 0 COMMENT '交易数量',
  `total_gas_used` BIGINT DEFAULT 0 COMMENT '总消耗Gas',
  `total_fee_wei` DECIMAL(30,0) DEFAULT 0 COMMENT '总费用（Wei）',
  `total_fee_eth` DECIMAL(20,8) DEFAULT 0 COMMENT '总费用（ETH）',
  `avg_gas_price` BIGINT DEFAULT 0 COMMENT '平均Gas价格',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date_enterprise` (`stat_date`, `enterprise_id`),
  KEY `idx_stat_date` (`stat_date`),
  KEY `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Gas费用统计表';

-- =============================================
-- 6. 用户端模块
-- =============================================

-- 6.1 用户扫码查询记录表
DROP TABLE IF EXISTS `user_scan_log`;
CREATE TABLE `user_scan_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `batch_code` VARCHAR(50) NOT NULL COMMENT '批次编号',
  `batch_id` BIGINT DEFAULT NULL COMMENT '批次ID',
  `openid` VARCHAR(100) DEFAULT NULL COMMENT '微信用户OpenID',
  `scan_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '扫码时间',
  `scan_location` VARCHAR(200) DEFAULT NULL COMMENT '扫码地点',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `device_type` VARCHAR(50) DEFAULT NULL COMMENT '设备类型',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  PRIMARY KEY (`id`),
  KEY `idx_batch_code` (`batch_code`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_scan_time` (`scan_time`),
  KEY `idx_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户扫码查询记录表';

-- 6.2 科普教育内容表
DROP TABLE IF EXISTS `education_content`;
CREATE TABLE `education_content` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
  `content` TEXT DEFAULT NULL COMMENT '内容',
  `author` VARCHAR(50) DEFAULT NULL COMMENT '作者',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架 1-上架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科普教育内容表';

-- 6.3 轮播图表
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '轮播图ID',
  `title` VARCHAR(100) DEFAULT NULL COMMENT '标题',
  `image_url` VARCHAR(255) NOT NULL COMMENT '图片URL',
  `link_type` TINYINT DEFAULT 0 COMMENT '链接类型：0-无 1-内部页面 2-外部链接',
  `link_url` VARCHAR(255) DEFAULT NULL COMMENT '链接地址',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮播图表';

-- =============================================
-- 7. 系统管理模块
-- =============================================

-- 7.1 系统参数配置表
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '参数ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '参数键',
  `config_value` TEXT DEFAULT NULL COMMENT '参数值',
  `config_type` VARCHAR(50) DEFAULT NULL COMMENT '参数类型',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '参数描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数配置表';

-- 7.2 操作日志表
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
  `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作内容',
  `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
  `params` TEXT DEFAULT NULL COMMENT '请求参数',
  `result` TEXT DEFAULT NULL COMMENT '返回结果',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `location` VARCHAR(200) DEFAULT NULL COMMENT '操作地点',
  `browser` VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
  `os` VARCHAR(100) DEFAULT NULL COMMENT '操作系统',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-失败 1-成功',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `execute_time` INT DEFAULT NULL COMMENT '执行时长（毫秒）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 7.3 登录日志表
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
  `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `location` VARCHAR(200) DEFAULT NULL COMMENT '登录地点',
  `browser` VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
  `os` VARCHAR(100) DEFAULT NULL COMMENT '操作系统',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-失败 1-成功',
  `message` VARCHAR(500) DEFAULT NULL COMMENT '提示信息',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- =============================================
-- 8. 数据字典表
-- =============================================

DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典类型ID',
  `dict_name` VARCHAR(100) NOT NULL COMMENT '字典名称',
  `dict_type` VARCHAR(100) NOT NULL COMMENT '字典类型',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
  `dict_type` VARCHAR(100) NOT NULL COMMENT '字典类型',
  `dict_label` VARCHAR(100) NOT NULL COMMENT '字典标签',
  `dict_value` VARCHAR(100) NOT NULL COMMENT '字典键值',
  `dict_sort` INT DEFAULT 0 COMMENT '排序',
  `css_class` VARCHAR(100) DEFAULT NULL COMMENT '样式属性',
  `list_class` VARCHAR(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认：0-否 1-是',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

-- =============================================
-- 9. 初始化数据
-- =============================================

-- 9.1 初始化管理员账号
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `user_type`, `status`, `create_time`) 
VALUES (1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE/TU.qj6E6ql2', '系统管理员', 1, 1, NOW());
-- 默认密码: admin123

-- 9.2 初始化角色
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `role_type`, `sort`, `status`, `create_time`) VALUES
(1, 'PLATFORM_ADMIN', '平台管理员', 1, 1, 1, NOW()),
(2, 'PLANTING_BREEDING', '种植养殖企业', 2, 2, 1, NOW()),
(3, 'PROCESSING', '加工宰杀企业', 3, 3, 1, NOW()),
(4, 'INSPECTION', '检疫质检企业', 4, 4, 1, NOW());

-- 9.3 初始化用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_time`) VALUES (1, 1, NOW());

-- 9.4 初始化菜单（平台管理端）
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

-- 9.5 初始化菜单（企业管理端）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`) VALUES
-- 企业信息管理（全员可见）
(1000, 0, '企业信息管理', 'enterprise:self', 1, '/enterprise/self', NULL, NULL, 'enterprise', 1, 1, 1),
(1001, 1000, '企业信息', 'enterprise:self:info', 2, '/enterprise/self/info', 'enterprise/self/info', 'enterprise:self:view', 'info', 1, 1, 1),

-- 生产管理（仅种植/养殖企业可见）
(1100, 0, '生产管理', 'production', 1, '/production', NULL, NULL, 'production', 2, 1, 1),
(1101, 1100, '种植/养殖初始化', 'production:init', 2, '/production/init', 'production/init/index', 'production:init:add', 'init', 1, 1, 1),
(1102, 1100, '过程信息记录', 'production:process', 2, '/production/process', 'production/process/index', 'production:process:add', 'process', 2, 1, 1),

-- 加工与来源（仅宰杀/加工企业可见）
(1200, 0, '加工与来源', 'processing', 1, '/processing', NULL, NULL, 'processing', 3, 1, 1),
(1201, 1200, '原料/加工记录', 'processing:material', 2, '/processing/material', 'processing/material/index', 'processing:material:add', 'material', 1, 1, 1),
(1202, 1200, '产品加工记录', 'processing:product', 2, '/processing/product', 'processing/product/index', 'processing:product:add', 'product', 2, 1, 1),

-- 质检与流通（检疫机构可见质检，全员可见仓储运输销售）
(1300, 0, '质检与流通', 'circulation', 1, '/circulation', NULL, NULL, 'circulation', 4, 1, 1),
(1301, 1300, '质量检测/检疫申报', 'circulation:inspection', 2, '/circulation/inspection', 'circulation/inspection/index', 'circulation:inspection:add', 'inspection', 1, 1, 1),
(1302, 1300, '仓储信息记录', 'circulation:storage', 2, '/circulation/storage', 'circulation/storage/index', 'circulation:storage:add', 'storage', 2, 1, 1),
(1303, 1300, '运输信息记录', 'circulation:transport', 2, '/circulation/transport', 'circulation/transport/index', 'circulation:transport:add', 'transport', 3, 1, 1),
(1304, 1300, '销售信息记录', 'circulation:sale', 2, '/circulation/sale', 'circulation/sale/index', 'circulation:sale:add', 'sale', 4, 1, 1),

-- 区块链（全员可见）
(1400, 0, '区块链', 'chain', 1, '/chain', NULL, NULL, 'chain', 5, 1, 1),
(1401, 1400, '区块链上链', 'chain:upload', 2, '/chain/upload', 'chain/upload/index', 'chain:upload:add', 'upload', 1, 1, 1),

-- 分析（全员可见）
(1500, 0, '数据统计分析', 'analysis', 1, '/analysis', NULL, NULL, 'analysis', 6, 1, 1),
(1501, 1500, '数据统计', 'analysis:statistics', 2, '/analysis/statistics', 'analysis/statistics/index', 'analysis:statistics:view', 'statistics', 1, 1, 1);

-- 9.6 初始化角色菜单关联（平台管理员拥有所有平台管理端菜单）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 1, id FROM `sys_menu` WHERE id BETWEEN 100 AND 699;

-- 9.7 初始化角色菜单关联（种植养殖企业）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
-- 企业信息管理
(2, 1000), (2, 1001),
-- 生产管理
(2, 1100), (2, 1101), (2, 1102),
-- 质检与流通（仓储、运输、销售）
(2, 1300), (2, 1302), (2, 1303), (2, 1304),
-- 区块链
(2, 1400), (2, 1401),
-- 分析
(2, 1500), (2, 1501);

-- 9.8 初始化角色菜单关联（加工宰杀企业）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
-- 企业信息管理
(3, 1000), (3, 1001),
-- 加工与来源
(3, 1200), (3, 1201), (3, 1202),
-- 质检与流通（仓储、运输、销售）
(3, 1300), (3, 1302), (3, 1303), (3, 1304),
-- 区块链
(3, 1400), (3, 1401),
-- 分析
(3, 1500), (3, 1501);

-- 9.9 初始化角色菜单关联（检疫质检企业）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
-- 企业信息管理
(4, 1000), (4, 1001),
-- 质检与流通（全部）
(4, 1300), (4, 1301), (4, 1302), (4, 1303), (4, 1304),
-- 区块链
(4, 1400), (4, 1401),
-- 分析
(4, 1500), (4, 1501);

-- 9.10 初始化字典类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`) VALUES
('用户类型', 'sys_user_type', 1),
('企业类型', 'enterprise_type', 1),
('审核状态', 'audit_status', 1),
('批次状态', 'batch_status', 1),
('记录类型', 'record_type', 1),
('加工类型', 'processing_type', 1),
('检疫类型', 'inspection_type', 1),
('检疫结果', 'inspection_result', 1),
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

-- 批次状态
('batch_status', '初始化', '1', 1, 1),
('batch_status', '生长中', '2', 2, 1),
('batch_status', '已收获', '3', 3, 1),
('batch_status', '加工中', '4', 4, 1),
('batch_status', '已检疫', '5', 5, 1),
('batch_status', '已入库', '6', 6, 1),
('batch_status', '运输中', '7', 7, 1),
('batch_status', '已销售', '8', 8, 1),

-- 记录类型
('record_type', '播种/投苗', '1', 1, 1),
('record_type', '施肥', '2', 2, 1),
('record_type', '浇水', '3', 3, 1),
('record_type', '用药', '4', 4, 1),
('record_type', '环境监测', '5', 5, 1),
('record_type', '日常巡查', '6', 6, 1),
('record_type', '其他', '7', 7, 1),

-- 加工类型
('processing_type', '宰杀', '1', 1, 1),
('processing_type', '分割', '2', 2, 1),
('processing_type', '包装', '3', 3, 1),
('processing_type', '深加工', '4', 4, 1),

-- 检疫类型
('inspection_type', '产地检疫', '1', 1, 1),
('inspection_type', '屠宰检疫', '2', 2, 1),
('inspection_type', '运输检疫', '3', 3, 1),
('inspection_type', '市场检疫', '4', 4, 1),
('inspection_type', '质量检测', '5', 5, 1),

-- 检疫结果
('inspection_result', '合格', '1', 1, 1),
('inspection_result', '不合格', '2', 2, 1),

-- 仓储类型
('storage_type', '入库', '1', 1, 1),
('storage_type', '出库', '2', 2, 1),
('storage_type', '库存盘点', '3', 3, 1),

-- 业务类型
('business_type', '批次', '1', 1, 1),
('business_type', '过程', '2', 2, 1),
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
('blockchain.private.key', '', 'blockchain', '私钥（加密存储）'),
('blockchain.gas.limit', '300000', 'blockchain', 'Gas限制'),
('blockchain.gas.price', '20', 'blockchain', 'Gas价格（Gwei）'),
('qrcode.base.url', 'https://your-domain.com/trace/', 'system', '溯源二维码基础URL'),
('file.upload.path', '/uploads/', 'system', '文件上传路径'),
('file.upload.max.size', '10485760', 'system', '文件上传最大大小（字节）');

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 数据库设计说明
-- =============================================
-- 1. RBAC权限设计：
--    - sys_user: 用户表，通过user_type和enterprise_id区分用户类型
--    - sys_role: 角色表，role_type对应企业类型（1-平台 2-种植养殖 3-加工 4-检疫）
--    - sys_menu: 菜单表，定义所有可访问的菜单和权限
--    - sys_user_role: 用户角色关联
--    - sys_role_menu: 角色菜单关联，实现动态权限控制
--
-- 2. 企业类型与权限映射：
--    - 种植养殖企业(type=1): 可访问生产管理、仓储运输销售
--    - 加工宰杀企业(type=2): 可访问加工管理、仓储运输销售
--    - 检疫质检企业(type=3): 可访问质检管理、仓储运输销售
--
-- 3. 溯源核心表设计：
--    - trace_batch: 批次主表，所有溯源数据的核心
--    - trace_process: 种植养殖过程记录
--    - trace_processing: 加工宰杀记录
--    - trace_inspection: 检疫质检记录
--    - trace_storage: 仓储记录
--    - trace_transport: 运输记录
--    - trace_sale: 销售记录
--    所有表通过batch_id关联，形成完整溯源链
--
-- 4. 区块链字段设计：
--    - tx_hash: 区块链交易哈希（66位，0x开头）
--    - block_number: 区块高度
--    - chain_time: 上链时间
--    - data_hash: 数据哈希值（用于验证数据完整性）
--    - message_id: 消息ID（用于链上查询）
--
-- 5. 数据验证流程：
--    - 业务数据生成data_hash
--    - 通过智能合约上链，获得tx_hash和message_id
--    - 用户查询时，通过message_id从链上获取哈希值
--    - 对比链上哈希与本地计算哈希，验证数据完整性
-- =============================================
