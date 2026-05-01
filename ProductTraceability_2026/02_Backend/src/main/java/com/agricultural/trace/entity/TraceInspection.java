package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 检疫质检记录表实体类
 * 对应数据库表: trace_inspection
 *
 * DB columns: id, batch_id, batch_code, inspection_date, check_result,
 *   inspection_items, cert_no, cert_image, inspector, inspector_code,
 *   inspector_id, inspection_enterprise_id, tx_hash, block_number,
 *   chain_time, data_hash, create_time, update_time, create_by, update_by, remark
 */
@Data
@TableName("trace_inspection")
public class TraceInspection {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次ID */
    private Long batchId;

    /** 批次编号 */
    private String batchCode;

    /** 检疫日期 */
    private LocalDate inspectionDate;

    /** 检疫结果：1-合格 0-不合格 */
    private Integer checkResult;

    /** 检测项目（JSON格式） */
    private String inspectionItems;

    /** 检疫证书编号 */
    private String certNo;

    /** 检疫证书图片(TEXT, JSON数组) */
    private String certImage;

    /** 检疫员姓名 */
    private String inspector;

    /** 检疫员工号 */
    private String inspectorCode;

    /** 检疫员用户ID */
    private Long inspectorId;

    /** 检疫机构ID */
    private Long inspectionEnterpriseId;

    /** 区块链交易哈希 */
    private String txHash;

    /** 区块高度 */
    private Long blockNumber;

    /** 上链时间 */
    private LocalDateTime chainTime;

    /** 数据哈希值 */
    private String dataHash;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private String remark;

    // ========== 兼容旧代码的别名方法 ==========

    /** 兼容: getInspectionResult -> checkResult */
    public Integer getInspectionResult() { return this.checkResult; }
    public void setInspectionResult(Integer val) { this.checkResult = val; }

    /** 兼容: getInspectionCertificateNo -> certNo */
    public String getInspectionCertificateNo() { return this.certNo; }
    public void setInspectionCertificateNo(String val) { this.certNo = val; }

    /** 兼容: getCertificateUrl -> certImage */
    public String getCertificateUrl() { return this.certImage; }
    public void setCertificateUrl(String val) { this.certImage = val; }

    /** 兼容: getImages -> certImage (同一字段) */
    public String getImages() { return this.certImage; }
    public void setImages(String val) { this.certImage = val; }

    /** 兼容: inspectionType 数据库无此列, 仅内存使用 */
    @TableField(exist = false)
    private Integer inspectionType;

    /** 兼容: inspectionData 数据库无此列 */
    @TableField(exist = false)
    private String inspectionData;

    /** 兼容: unqualifiedReason 数据库无此列 */
    @TableField(exist = false)
    private String unqualifiedReason;

    /** 兼容: handleMethod 数据库无此列 */
    @TableField(exist = false)
    private String handleMethod;
}
