package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 企业信息实体
 */
@Data
@TableName("enterprise_info")
public class EnterpriseInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String enterpriseCode;

    private String enterpriseName;

    /**
     * 企业类型：1-种植养殖 2-加工宰杀 3-检疫质检
     */
    private Integer enterpriseType;

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

    private String logo;

    private String coverImage;

    /**
     * 审核状态：0-待审核 1-审核通过 2-审核拒绝
     */
    private Integer auditStatus;

    private String auditRemark;

    private LocalDateTime auditTime;

    private Long auditBy;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private String remark;

    @TableField(exist = false)
    private Long chainCount;

    @TableField(exist = false)
    private Long batchCount;

    @TableField(exist = false)
    private Long verifyCount;

    @TableField(exist = false)
    private Long productCount;

    @TableField(exist = false)
    private String lastChainTime;
}
