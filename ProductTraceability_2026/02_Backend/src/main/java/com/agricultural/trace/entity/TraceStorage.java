package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 仓储记录表实体类
 */
@Data
@TableName("trace_storage")
public class TraceStorage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次ID */
    private Long batchId;

    /** 批次编号 */
    private String batchCode;

    /** 仓储企业ID */
    private Long storageEnterpriseId;

    /** 前端传入的enterpriseId（不映射数据库列） */
    @TableField(exist = false)
    private Long enterpriseId;

    /** 仓储类型: 1-入库 2-出库 3-库存盘点 */
    private Integer storageType;

    /** 仓储日期 */
    private LocalDate storageDate;

    /** 仓库名称 */
    private String warehouseName;

    /** 仓库位置 */
    private String warehouseLocation;

    /** 仓储数量 */
    private BigDecimal storageQuantity;

    /** 单位 */
    private String storageUnit;

    /** 仓库温度(摄氏度) */
    private BigDecimal temperature;

    /** 仓库湿度(%) */
    private BigDecimal humidity;

    /** 存储条件说明 */
    private String storageCondition;

    /** 操作人 */
    private String operator;

    /** 图片URL(JSON数组) */
    private String images;

    /** 区块链交易哈希 */
    private String txHash;

    /** 区块高度 */
    private Long blockNumber;

    /** 上链时间 */
    private LocalDateTime chainTime;

    /** 数据哈希值 */
    private String dataHash;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private String remark;
}
