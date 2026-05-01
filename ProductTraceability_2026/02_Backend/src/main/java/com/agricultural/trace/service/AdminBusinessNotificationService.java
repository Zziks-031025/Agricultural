package com.agricultural.trace.service;

import com.agricultural.trace.dto.BatchReceiveDTO;
import com.agricultural.trace.dto.QuarantineSubmitDTO;
import com.agricultural.trace.entity.EnterpriseInfo;
import com.agricultural.trace.entity.TraceBatch;
import com.agricultural.trace.entity.TraceProcessing;
import com.agricultural.trace.entity.TraceRecord;
import com.agricultural.trace.entity.TraceStorage;
import com.agricultural.trace.entity.TraceTransport;
import com.agricultural.trace.mapper.EnterpriseInfoMapper;
import com.agricultural.trace.mapper.TraceBatchMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBusinessNotificationService {

    private final MessageService messageService;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final TraceBatchMapper traceBatchMapper;

    public void notifyBatchCreated(Map<String, Object> params, Map<String, Object> result) {
        Long enterpriseId = toLong(params.get("enterpriseId"));
        String batchCode = toText(result.get("batchCode"));
        String productName = toText(params.get("productName"));
        if (!StringUtils.hasText(productName)) {
            productName = "未命名产品";
        }
        send(
                "批次初始化已提交",
                enterprisePrefix(enterpriseId) + "创建了批次 " + batchCode,
                "企业名称：" + resolveEnterpriseName(enterpriseId)
                        + "\n批次编号：" + batchCode
                        + "\n产品名称：" + productName
                        + "\n请及时关注后续流转。",
                "/supervision/trace"
        );
    }

    public void notifyRecordCreated(TraceRecord record) {
        TraceBatch batch = resolveBatch(record.getBatchId(), record.getBatchCode());
        if (batch == null) {
            return;
        }
        send(
                "生长记录已新增",
                enterprisePrefix(batch.getEnterpriseId()) + "新增了批次 " + batch.getBatchCode() + " 的生长记录",
                "企业名称：" + resolveEnterpriseName(batch.getEnterpriseId())
                        + "\n批次编号：" + batch.getBatchCode()
                        + "\n记录类型：" + nullToPlaceholder(record.getRecordType())
                        + "\n记录项目：" + nullToPlaceholder(record.getItemName()),
                "/supervision/trace"
        );
    }

    public void notifyQuarantineApplied(Map<String, Object> params, Map<String, Object> result) {
        TraceBatch batch = resolveBatch(toLong(result.get("batchId")), toText(result.get("batchCode")));
        if (batch == null) {
            return;
        }
        Long inspectionEnterpriseId = toLong(result.get("inspectionEnterpriseId"));
        send(
                "检疫申报已提交",
                enterprisePrefix(batch.getEnterpriseId()) + "提交了批次 " + batch.getBatchCode() + " 的检疫申报",
                "来源企业：" + resolveEnterpriseName(batch.getEnterpriseId())
                        + "\n批次编号：" + batch.getBatchCode()
                        + "\n目标检疫企业：" + resolveEnterpriseName(inspectionEnterpriseId)
                        + "\n备注：" + nullToPlaceholder(toText(params.get("remark"))),
                "/supervision/trace"
        );
    }

    public void notifyQuarantineSubmitted(QuarantineSubmitDTO dto, Map<String, Object> result) {
        TraceBatch batch = resolveBatch(toLong(result.get("batchId")), toText(result.get("batchCode")));
        if (batch == null) {
            return;
        }
        String resultText = Integer.valueOf(1).equals(dto.getCheckResult()) ? "合格" : "不合格";
        send(
                "检疫审核结果已提交",
                enterprisePrefix(dto.getEnterpriseId()) + "提交了批次 " + batch.getBatchCode() + " 的检疫结果",
                "来源企业：" + resolveEnterpriseName(batch.getEnterpriseId())
                        + "\n检疫企业：" + resolveEnterpriseName(dto.getEnterpriseId())
                        + "\n批次编号：" + batch.getBatchCode()
                        + "\n审核结果：" + resultText
                        + "\n证书编号：" + nullToPlaceholder(dto.getEffectiveCertificateNo()),
                "/supervision/trace"
        );
    }

    public void notifyStorageCreated(TraceStorage record) {
        TraceBatch batch = resolveBatch(record.getBatchId(), record.getBatchCode());
        if (batch == null) {
            return;
        }
        send(
                "仓储记录已新增",
                enterprisePrefix(record.getStorageEnterpriseId()) + "提交了批次 " + batch.getBatchCode() + " 的仓储记录",
                "来源企业：" + resolveEnterpriseName(batch.getEnterpriseId())
                        + "\n仓储企业：" + resolveEnterpriseName(record.getStorageEnterpriseId())
                        + "\n批次编号：" + batch.getBatchCode()
                        + "\n仓库名称：" + nullToPlaceholder(record.getWarehouseName()),
                "/supervision/trace"
        );
    }

    public void notifyTransportCreated(TraceTransport record) {
        TraceBatch batch = resolveBatch(record.getBatchId(), record.getBatchCode());
        if (batch == null) {
            return;
        }
        send(
                "运输记录已新增",
                enterprisePrefix(record.getTransportEnterpriseId()) + "提交了批次 " + batch.getBatchCode() + " 的运输记录",
                "来源企业：" + resolveEnterpriseName(batch.getEnterpriseId())
                        + "\n运输企业：" + resolveEnterpriseName(record.getTransportEnterpriseId())
                        + "\n接收企业：" + resolveEnterpriseName(record.getReceiveEnterpriseId())
                        + "\n批次编号：" + batch.getBatchCode()
                        + "\n目的地：" + nullToPlaceholder(record.getDestination()),
                "/supervision/trace"
        );
    }

    public void notifyProcessingCreated(TraceProcessing record) {
        TraceBatch batch = resolveBatch(record.getBatchId(), record.getSourceBatchCode());
        if (batch == null) {
            return;
        }
        send(
                "加工记录已新增",
                enterprisePrefix(record.getProcessingEnterpriseId()) + "提交了批次 " + batch.getBatchCode() + " 的加工记录",
                "来源企业：" + resolveEnterpriseName(batch.getEnterpriseId())
                        + "\n加工企业：" + resolveEnterpriseName(record.getProcessingEnterpriseId())
                        + "\n批次编号：" + batch.getBatchCode()
                        + "\n加工方式：" + nullToPlaceholder(record.getProcessMethod()),
                "/supervision/trace"
        );
    }

    public void notifyBatchReceived(BatchReceiveDTO dto, Map<String, Object> result) {
        TraceBatch batch = resolveBatch(null, dto.getBatchCode());
        if (batch == null) {
            return;
        }
        send(
                "原料接收已完成",
                enterprisePrefix(dto.getEnterpriseId()) + "完成了批次 " + batch.getBatchCode() + " 的接收",
                "来源企业：" + resolveEnterpriseName(batch.getEnterpriseId())
                        + "\n接收企业：" + resolveEnterpriseName(dto.getEnterpriseId())
                        + "\n批次编号：" + batch.getBatchCode()
                        + "\n接收人：" + nullToPlaceholder(dto.getReceiver())
                        + "\n接收数量：" + nullToPlaceholder(dto.getReceiveQuantity()),
                "/supervision/trace"
        );
    }

    public void notifyBatchReceiveCancelled(Long batchId) {
        TraceBatch batch = resolveBatch(batchId, null);
        if (batch == null) {
            return;
        }
        send(
                "原料接收已取消",
                enterprisePrefix(batch.getReceiveEnterpriseId()) + "取消了批次 " + batch.getBatchCode() + " 的接收",
                "来源企业：" + resolveEnterpriseName(batch.getEnterpriseId())
                        + "\n接收企业：" + resolveEnterpriseName(batch.getReceiveEnterpriseId())
                        + "\n批次编号：" + batch.getBatchCode(),
                "/supervision/trace"
        );
    }

    public void notifyBatchRejected(Long batchId, String reason, Long rejectEnterpriseId) {
        TraceBatch batch = resolveBatch(batchId, null);
        if (batch == null) {
            return;
        }
        send(
                "原料接收被拒绝",
                enterprisePrefix(rejectEnterpriseId) + "拒绝接收批次 " + batch.getBatchCode(),
                "来源企业：" + resolveEnterpriseName(batch.getEnterpriseId())
                        + "\n拒收企业：" + resolveEnterpriseName(rejectEnterpriseId)
                        + "\n批次编号：" + batch.getBatchCode()
                        + "\n拒绝原因：" + nullToPlaceholder(reason),
                "/supervision/trace"
        );
    }

    private void send(String title, String summary, String content, String actionUrl) {
        try {
            messageService.sendToAdmins("business", title, summary, content, actionUrl, "查看详情", "前往处理");
        } catch (Exception e) {
            log.warn("send admin business notification failed: {}", e.getMessage());
        }
    }

    private TraceBatch resolveBatch(Long batchId, String batchCode) {
        if (batchId != null) {
            return traceBatchMapper.selectById(batchId);
        }
        if (!StringUtils.hasText(batchCode)) {
            return null;
        }
        LambdaQueryWrapper<TraceBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TraceBatch::getBatchCode, batchCode.trim()).last("LIMIT 1");
        return traceBatchMapper.selectOne(wrapper);
    }

    private String resolveEnterpriseName(Long enterpriseId) {
        if (enterpriseId == null) {
            return "未关联企业";
        }
        EnterpriseInfo enterpriseInfo = enterpriseInfoMapper.selectById(enterpriseId);
        if (enterpriseInfo == null || !StringUtils.hasText(enterpriseInfo.getEnterpriseName())) {
            return "企业#" + enterpriseId;
        }
        return enterpriseInfo.getEnterpriseName();
    }

    private String enterprisePrefix(Long enterpriseId) {
        return "企业「" + resolveEnterpriseName(enterpriseId) + "」";
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            return Long.parseLong(((String) value).trim());
        }
        return null;
    }

    private String toText(Object value) {
        return value == null ? null : value.toString();
    }

    private String nullToPlaceholder(Object value) {
        if (value == null) {
            return "--";
        }
        String text = value.toString();
        return StringUtils.hasText(text) ? text : "--";
    }
}
