package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批次表实体类
 */
@Data
@TableName("trace_batch")
public class TraceBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次编号（唯一溯源码） */
    private String batchCode;

    /** 使用的模版ID (数据库中暂无此列, 不映射) */
    @TableField(exist = false)
    private Long templateId;

    /** 产品名称 */
    private String productName;

    /** 产品类型：1-肉鸡 2-西红柿 */
    private Integer productType;

    /** 品种 */
    private String breed;

    /** 产地位置 */
    private String originLocation;

    /** 纬度 */
    private java.math.BigDecimal latitude;

    /** 经度 */
    private java.math.BigDecimal longitude;

    /** 种植面积 */
    private BigDecimal plantArea;

    /** 大棚编号 */
    private String greenhouseNo;

    /** 苗源/种子来源 */
    private String seedSource;

    /** 负责人 */
    private String manager;

    /** 创建企业ID */
    private Long enterpriseId;

    /** 溯源二维码URL */
    private String qrCodeUrl;

    /** 初始数量 */
    private BigDecimal initQuantity;

    /** 当前数量 */
    private BigDecimal currentQuantity;

    /** 单位 */
    private String unit;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 预计收获/出栏日期 */
    private LocalDate expectedHarvestDate;

    /** 实际收获/出栏日期 */
    private LocalDate actualHarvestDate;

    /**
     * 批次状态：
     * 1-初始化 2-生长中 3-已收获 4-加工中 
     * 5-已检疫 6-已入库 7-运输中 8-已销售
     */
    private Integer batchStatus;

    /** 区块链交易哈希 */
    private String txHash;

    /** 区块高度 */
    private Long blockNumber;

    /** 上链时间 */
    private LocalDateTime chainTime;

    /** 数据哈希值（用于验证） */
    private String dataHash;

    /** 接收人 */
    private String receiver;

    /** 接收日期 */
    private LocalDate receiveDate;

    /** 接收企业ID（加工企业） */
    private Long receiveEnterpriseId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private String remark;
}
