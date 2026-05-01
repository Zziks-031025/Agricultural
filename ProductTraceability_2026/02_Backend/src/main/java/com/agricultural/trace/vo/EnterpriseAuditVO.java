package com.agricultural.trace.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业审核VO
 */
@Data
public class EnterpriseAuditVO {

    private Long id;

    private String enterpriseCode;

    private String enterpriseName;

    private Integer enterpriseType;

    private String enterpriseTypeName;

    private String legalPerson;

    private String contactPerson;

    private String contactPhone;

    private String contactEmail;

    private String province;

    private String city;

    private String district;

    private String address;

    private String businessLicense;

    private String productionLicense;

    private String otherCertificates;

    private String introduction;

    private Integer auditStatus;

    private String auditStatusName;

    private String auditRemark;

    private LocalDateTime auditTime;

    private String auditByName;

    private LocalDateTime createTime;
}
