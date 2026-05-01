package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 销售记录表实体类
 */
@Data
@TableName("trace_sale")
public class TraceSale {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次ID */
    private Long batchId;

    /** 批次编号 */
    private String batchCode;

    /** 销售企业ID */
    private Long saleEnterpriseId;

    /** 前端传入的enterpriseId（不映射数据库列） */
    @TableField(exist = false)
    private Long enterpriseId;

    /** 销售日期 */
    private LocalDate saleDate;

    /** 销售时间 */
    private LocalTime saleTime;

    /** 销售对象（如超市/市场名称） */
    private String buyerName;

    /** 销售数量 */
    private BigDecimal saleQuantity;

    /** 单位 */
    private String saleUnit;

    /** 销售单价 */
    private BigDecimal salePrice;

    /** 销售总额 */
    private BigDecimal totalAmount;

    /** 销售凭证图片（JSON数组） */
    private String saleVoucher;

    /** 销售渠道 */
    private String saleChannel;

    /** 销售目的地 */
    private String destination;

    /** 区块链交易哈希 */
    private String txHash;

    /** 区块高度 */
    private Long blockNumber;

    /** 上链时间 */
    private LocalDateTime chainTime;

    /** 数据哈希值（用于验证） */
    private String dataHash;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private String remark;
}
