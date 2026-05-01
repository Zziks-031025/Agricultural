package com.agricultural.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 运输记录表实体类
 */
@Data
@TableName("trace_transport")
public class TraceTransport {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次ID */
    private Long batchId;

    /** 批次编号 */
    private String batchCode;

    /** 物流单号 */
    private String logisticsNo;

    /** 运输企业ID */
    private Long transportEnterpriseId;

    /** 目标接收企业ID（加工企业） */
    private Long receiveEnterpriseId;

    /** 前端传入的enterpriseId（不映射数据库列） */
    @TableField(exist = false)
    private Long enterpriseId;

    /** 运输日期 */
    private LocalDate transportDate;

    /** 车牌号 */
    private String plateNumber;

    /** 司机姓名 */
    private String driverName;

    /** 司机电话 */
    private String driverPhone;

    /** 收货人姓名 */
    private String receiverName;

    /** 出发地 */
    private String departureLocation;

    /** 目的地 */
    private String destination;

    /** 出发时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime departureTime;

    /** 到达时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime arrivalTime;

    /** 运输数量 */
    private BigDecimal transportQuantity;

    /** 运输单位 */
    private String transportUnit;

    /** 温度(摄氏度) */
    private BigDecimal temperature;

    /** 湿度(%) */
    private BigDecimal humidity;

    /** 运输条件说明 */
    private String transportCondition;

    /** GPS轨迹(JSON格式) */
    private String gpsTrack;

    /** 图片URL(JSON数组) */
    private String images;

    /** 区块链交易哈希 */
    private String txHash;

    /** 区块高度 */
    private Long blockNumber;

    /** 上链时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime chainTime;

    /** 数据哈希值（用于验证） */
    private String dataHash;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private String remark;
}
