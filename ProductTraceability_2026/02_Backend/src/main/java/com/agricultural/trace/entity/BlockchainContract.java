package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("blockchain_contract")
public class BlockchainContract implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String contractName;

    private String contractAddress;

    private String contractAbi;

    private String contractCode;

    private String deployTxHash;

    private Long deployBlockNumber;

    private String network;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private String remark;
}
