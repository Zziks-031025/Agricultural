package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 区块链交易记录实体
 */
@Data
@TableName("blockchain_transaction")
public class BlockchainTransaction implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String txHash;

    private String messageId;

    /** 业务类型: 1-批次 2-生长记录 3-检疫 4-加工 5-仓储 6-运输 7-销售 */
    private Integer businessType;

    private Long businessId;

    private Long batchId;

    private String dataHash;

    private String contractAddress;

    private String fromAddress;

    private Long blockNumber;

    private String blockHash;

    private Long gasUsed;

    /** Gas价格(Wei) */
    private Long gasPrice;

    /** 交易费用(ETH) */
    private BigDecimal transactionFee;

    /** 状态: 0-待确认 1-成功 2-失败 */
    private Integer status;

    private String errorMessage;

    private LocalDateTime chainTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
