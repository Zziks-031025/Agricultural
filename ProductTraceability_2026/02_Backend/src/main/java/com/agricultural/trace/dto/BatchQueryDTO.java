package com.agricultural.trace.dto;

import lombok.Data;

import java.util.List;

/**
 * 批次查询 DTO
 * 支持多状态查询
 */
@Data
public class BatchQueryDTO {

    /** 当前页 */
    private Long current = 1L;

    /** 每页大小 */
    private Long size = 10L;

    /** 企业ID（用于筛选指定企业的批次） */
    private Long enterpriseId;

    /** 企业类型：1-养殖 2-加工 3-检疫，用于判断查询逻辑 */
    private Integer enterpriseType;

    /** 产品名称（模糊搜索） */
    private String productName;

    /** 批次编号（模糊搜索） */
    private String batchCode;

    /** 搜索关键词（同时搜索批次号和产品名称） */
    private String keyword;

    /**
     * 状态列表（支持多状态查询）
     * 
     * Tab 映射关系：
     * - 养殖中: [1, 2]（初始化 + 生长中）
     * - 待检疫: [3]（已收获）
     * - 已完结: [4, 5, 6, 7, 8]（加工中、已检疫、已入库、运输中、已销售）
     */
    private List<Integer> statuses;

    /** 产品类型：1-种植 2-养殖 */
    private Integer productType;

    /** 是否已上链：true-已上链, false-未上链, null-全部 */
    private Boolean hasChain;

    private Boolean excludeStorageEntered;

    private Boolean excludeProcessedCreated;

    /** 检疫机构ID：检疫企业查询分配给本机构的批次（通过 trace_inspection 关联） */
    private Long inspectionEnterpriseId;
}
