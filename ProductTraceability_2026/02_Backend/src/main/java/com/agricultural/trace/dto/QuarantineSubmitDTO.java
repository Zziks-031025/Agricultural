package com.agricultural.trace.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 检疫结果提交 DTO
 * 用于 /api/quarantine/submit 接口
 */
@Data
public class QuarantineSubmitDTO {

    /** 批次ID (optional if batchCode is provided) */
    private Long batchId;

    /** 批次编号 (frontend sends this, backend resolves to batchId) */
    private String batchCode;

    /** 检疫结果：1-合格 0-不合格 */
    @NotNull(message = "检疫结果不能为空")
    private Integer checkResult;

    /** 检疫员ID (optional if inspector name provided) */
    private Long inspectorId;

    /** 检疫员姓名 (from frontend operator field) */
    private String inspector;

    /** 检疫员工号 */
    private String inspectorCode;

    /** 检疫日期 (YYYY-MM-DD) */
    private String inspectionDate;

    /** 照片路径 */
    private String imagePath;

    /** 检疫证书图片 (JSON array) */
    private String certImage;

    /** 检疫机构ID */
    private Long enterpriseId;

    /** 检疫证书编号 */
    private String certificateNo;

    /** 检疫证书编号 (alias for frontend compatibility) */
    private String certNo;

    /** 检测项目（JSON格式，如：禽流感检测、药物残留、感官指标） */
    private String inspectionItems;

    /** 检疫类型：1-产地检疫 2-屠宰检疫 3-运输检疫 4-市场检疫 5-质量检测 */
    private Integer inspectionType;

    /** 不合格原因（checkResult=2 时使用） */
    private String unqualifiedReason;

    /** 备注 */
    private String remark;

    /**
     * Get effective certificate number (supports both field names)
     */
    public String getEffectiveCertificateNo() {
        if (certificateNo != null && !certificateNo.trim().isEmpty()) {
            return certificateNo;
        }
        return certNo;
    }
}
