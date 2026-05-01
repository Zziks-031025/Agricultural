package com.agricultural.trace.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 批次接收 DTO
 */
@Data
public class BatchReceiveDTO {

    /** 批次编号 */
    @NotBlank(message = "批次编号不能为空")
    private String batchCode;

    /** 接收数量 */
    @NotNull(message = "接收数量不能为空")
    private BigDecimal receiveQuantity;

    /** 接收人 */
    @NotBlank(message = "接收人不能为空")
    private String receiver;

    /** 接收企业ID */
    private Long enterpriseId;

    /** 接收日期 (yyyy-MM-dd) */
    private String receiveDate;

    /** 备注 */
    private String remark;
}
