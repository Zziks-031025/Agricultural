package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业图片审核表实体
 * 用于审核头像、营业执照、行业许可证、企业logo的变更
 */
@Data
@TableName("enterprise_audit_image")
public class EnterpriseAuditImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业ID */
    private Long enterpriseId;

    /** 提交用户ID */
    private Long userId;

    /** 字段名: avatar/business_license/production_license/logo */
    private String fieldName;

    /** 旧图片URL */
    private String oldValue;

    /** 新图片URL */
    private String newValue;

    /** 审核状态: 0-待审核 1-通过 2-拒绝 */
    private Integer auditStatus;

    /** 审核备注(拒绝原因) */
    private String auditRemark;

    /** 审核人ID */
    private Long auditBy;

    /** 审核时间 */
    private LocalDateTime auditTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
