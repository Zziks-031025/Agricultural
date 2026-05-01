package com.agricultural.trace.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 批次列表 VO
 */
@Data
public class BatchVO {

    private Long id;

    /** 批次编号 */
    private String batchCode;

    /** 产品名称 */
    private String productName;

    /** 产品类型：1-肉鸡 2-西红柿 */
    private Integer productType;

    /** 品种 */
    private String breed;

    /** 初始数量 */
    private BigDecimal initQuantity;

    /** 当前数量 */
    private BigDecimal quantity;

    /** 单位 */
    private String unit;

    /** 企业ID */
    private Long enterpriseId;

    /** 企业名称 */
    private String enterpriseName;

    /** 企业类型 */
    private Integer enterpriseType;

    /**
     * 批次状态（数据库状态码）
     * 1-初始化 2-生长中 3-已收获 4-加工中 
     * 5-已检疫 6-已入库 7-运输中 8-已销售
     */
    private Integer status;

    /** 状态名称（显示用） */
    private String statusText;

    /** 创建时间 */
    private String createTime;

    /** 创建日期（格式化为 YYYY-MM-DD） */
    private String createDate;

    /** 区块链交易哈希 */
    private String txHash;

    /** 数据哈希 (SHA-256) */
    private String dataHash;

    /** 哈希外显格式（前6位+后6位） */
    private String txHashDisplay;

    /** 是否已上链 */
    private Boolean hasChain;

    /** 二维码URL */
    private String qrCodeUrl;

    /** 接收人 */
    private String receiver;

    /** 接收日期 */
    private String receiveDate;

    /** 更新时间 */
    private String updateTime;

    private Boolean quarantinePassed;

    // ========== 状态名称映射 ==========

    /**
     * 获取状态名称
     */
    public String getStatusText() {
        if (this.statusText != null) {
            return this.statusText;
        }
        if (this.status == null) {
            return "未知";
        }
        switch (this.status) {
            case 1: return "初始化";
            case 2: return "生长中";
            case 3: return "已收获";
            case 4: return "加工中";
            case 5: return "已检疫";
            case 6: return "已入库";
            case 7: return "运输中";
            case 8: return "已销售";
            case 9: return "加工完成";
            default: return "未知";
        }
    }

    /**
     * 格式化哈希外显
     */
    public String getTxHashDisplay() {
        if (this.txHashDisplay != null) {
            return this.txHashDisplay;
        }
        if (this.txHash == null || this.txHash.length() < 20) {
            return this.txHash;
        }
        return this.txHash.substring(0, 8) + "..." + this.txHash.substring(this.txHash.length() - 6);
    }

    /**
     * 判断是否已上链
     */
    public Boolean getHasChain() {
        if (this.hasChain != null) {
            return this.hasChain;
        }
        return this.txHash != null && !this.txHash.isEmpty();
    }
}
