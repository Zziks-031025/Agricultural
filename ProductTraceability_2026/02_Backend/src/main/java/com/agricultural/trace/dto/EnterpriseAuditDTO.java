package com.agricultural.trace.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 企业审核DTO
 */
@Data
public class EnterpriseAuditDTO {

    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;

    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String auditRemark;
}
