package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Gas费用日统计实体
 */
@Data
@TableName("blockchain_gas_fee")
public class BlockchainGasFee implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;

    private Long enterpriseId;

    private Integer transactionCount;

    private Long totalGasUsed;

    /** 总费用(Wei) */
    private BigDecimal totalFeeWei;

    /** 总费用(ETH) */
    private BigDecimal totalFeeEth;

    private Long avgGasPrice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
