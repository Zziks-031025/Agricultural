package com.agricultural.trace.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {

    private Long id;

    private String username;

    private String realName;

    private String avatar;

    private Integer userType;

    /** 企业ID */
    private Long enterpriseId;

    /** 企业类型：1-种植养殖 2-加工宰杀 3-检疫质检 */
    private Integer enterpriseType;

    /** 企业名称 */
    private String enterpriseName;

    /** 企业审核状态：0-待审核 1-审核通过 2-审核驳回 */
    private Integer enterpriseAuditStatus;

    /** 企业审核备注 */
    private String auditRemark;

    /** 企业状态：0-禁用 1-启用 */
    private Integer enterpriseStatus;

    private List<String> roles;

    private List<String> permissions;
}
