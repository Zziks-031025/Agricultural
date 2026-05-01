package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生长/操作记录表实体类
 */
@Data
@TableName("trace_record")
public class TraceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次ID */
    private Long batchId;

    /** 批次编号（冗余） */
    private String batchCode;

    /** 记录类型: feeding/vaccine/inspect/fertilize/irrigate/pesticide */
    private String recordType;

    /** 记录日期 */
    private LocalDate recordDate;

    /** 项目名称 */
    private String itemName;

    /** 用量 */
    private java.math.BigDecimal amount;

    /** 描述 */
    private String description;

    /** 操作人 */
    private String operator;

    /** 图片(JSON数组) */
    private String images;

    /** 位置 */
    private String location;

    /** 纬度 */
    private BigDecimal latitude;

    /** 经度 */
    private BigDecimal longitude;

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

    /** 软删除标记 0-正常 1-已删除 */
    @TableLogic
    private Integer deleted;

    /** 删除时间 */
    private LocalDateTime deleteTime;
}
