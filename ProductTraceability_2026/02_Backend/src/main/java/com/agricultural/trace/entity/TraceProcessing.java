package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 加工信息表实体类
 */
@Data
@TableName("trace_processing")
public class TraceProcessing {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次ID */
    private Long batchId;

    /** 原料批次码 */
    private String sourceBatchCode;

    /** 加工企业ID */
    private Long processingEnterpriseId;

    /** 前端传入的enterpriseId（不映射数据库列） */
    @TableField(exist = false)
    private Long enterpriseId;

    /** 加工日期 */
    private LocalDate processingDate;

    /** 加工方式 */
    private String processMethod;

    /** 包装规格 */
    private String specs;

    /** 操作员 */
    private String operator;

    /** 投入数量 */
    private BigDecimal inputQuantity;

    /** 投入数量单位 */
    private String inputUnit;

    /** 产出数量 */
    private BigDecimal outputQuantity;

    /** 产出数量单位 */
    private String outputUnit;

    /** 现场照片(JSON数组) */
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
