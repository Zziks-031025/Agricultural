package com.agricultural.trace.service;

import com.agricultural.trace.entity.EnterpriseInfo;
import com.agricultural.trace.entity.TraceBatch;
import com.agricultural.trace.entity.TraceProcessing;
import com.agricultural.trace.mapper.EnterpriseInfoMapper;
import com.agricultural.trace.mapper.TraceBatchMapper;
import com.agricultural.trace.mapper.TraceProcessingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 加工记录服务
 * - 支持 sourceBatchCode -> batchId 自动解析
 * - 自动填充 processingEnterpriseId
 * - 提交加工记录后更新批次状态为 5 (加工完成)
 * - 上链统一在销售记录提交时执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingService {

    private final TraceProcessingMapper traceProcessingMapper;
    private final TraceBatchMapper traceBatchMapper;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final com.agricultural.trace.utils.Web3jUtils web3jUtils;
    private final BlockchainTransactionService blockchainTransactionService;

    /**
     * 创建加工记录
     */
    @Transactional(rollbackFor = Exception.class)
    public TraceProcessing createProcessing(TraceProcessing record) {
        // Resolve sourceBatchCode -> batchId
        TraceBatch batch = null;
        if (record.getBatchId() == null && StringUtils.hasText(record.getSourceBatchCode())) {
            batch = findBatchByCode(record.getSourceBatchCode());
            record.setBatchId(batch.getId());
        } else if (record.getBatchId() != null) {
            batch = traceBatchMapper.selectById(record.getBatchId());
        }

        if (record.getBatchId() != null) {
            LambdaQueryWrapper<TraceProcessing> duplicateWrapper = new LambdaQueryWrapper<>();
            duplicateWrapper.eq(TraceProcessing::getBatchId, record.getBatchId());
            if (traceProcessingMapper.selectCount(duplicateWrapper) > 0) {
                throw new RuntimeException("该批次已存在加工记录");
            }
        }

        if (record.getProcessingEnterpriseId() == null && batch != null) {
            log.debug("processingEnterpriseId not set, will use frontend-injected value if available");
        }

        // 加工记录需独立上链，不继承批次的上链信息

        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        traceProcessingMapper.insert(record);

        // Update batch status to 9 (processing complete)
        if (batch != null) {
            batch.setBatchStatus(9);
            batch.setUpdateTime(LocalDateTime.now());
            traceBatchMapper.updateById(batch);
            log.info("batch status updated to 9 (processing complete), batchCode={}", batch.getBatchCode());
        }

        // ====== 加工记录独立上链 ======
        try {
            String dataHash = computeSHA256("PROCESSING|batchId=" + record.getBatchId()
                    + "|method=" + record.getProcessMethod()
                    + "|specs=" + record.getSpecs()
                    + "|in=" + record.getInputQuantity()
                    + "|out=" + record.getOutputQuantity()
                    + "|id=" + record.getId()
                    + "|ts=" + System.currentTimeMillis());
            String traceId = record.getSourceBatchCode() + "_PROC_" + record.getId();
            String txHash = web3jUtils.uploadHash(traceId, dataHash);

            Long blockNumber = null;
            try {
                org.web3j.protocol.core.methods.response.TransactionReceipt receipt =
                        web3jUtils.getTransactionReceipt(txHash);
                if (receipt != null) {
                    blockNumber = receipt.getBlockNumber().longValue();
                }
            } catch (Exception ignored) {}

            LocalDateTime chainTime = LocalDateTime.now();
            record.setTxHash(txHash);
            record.setDataHash(dataHash);
            record.setBlockNumber(blockNumber);
            record.setChainTime(chainTime);
            record.setUpdateTime(LocalDateTime.now());
            traceProcessingMapper.updateById(record);

            log.info("加工记录上链成功, id={}, txHash={}, blockNumber={}", record.getId(), txHash, blockNumber);

            // 记录交易到 blockchain_transaction 表
            org.web3j.protocol.core.methods.response.TransactionReceipt fullReceipt =
                    web3jUtils.getTransactionReceipt(txHash);
            blockchainTransactionService.recordTransaction(txHash,
                    BlockchainTransactionService.BIZ_PROCESSING, record.getId(),
                    record.getBatchId(), dataHash, fullReceipt);
        } catch (Exception e) {
            log.warn("加工记录上链失败(不影响业务): {}", e.getMessage());
        }

        log.info("processing record created, id={}, sourceBatchCode={}, batchId={}, method={}",
                record.getId(), record.getSourceBatchCode(), record.getBatchId(), record.getProcessMethod());
        return record;
    }

    /**
     * 更新加工记录
     */
    public void updateProcessing(TraceProcessing record) {
        if (record.getProcessingEnterpriseId() == null && record.getEnterpriseId() != null) {
            record.setProcessingEnterpriseId(record.getEnterpriseId());
        }
        record.setUpdateTime(java.time.LocalDateTime.now());
        traceProcessingMapper.updateById(record);
        log.info("更新加工记录, id={}, batchId={}", record.getId(), record.getBatchId());
    }

    /**
     * 分页查询加工记录 (支持 enterpriseId + keyword 筛选)
     */
    public Page<Map<String, Object>> getProcessingList(Long pageNum, Long pageSize,
                                                        Long batchId, Long enterpriseId,
                                                        String keyword) {
        Page<TraceProcessing> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TraceProcessing> wrapper = new LambdaQueryWrapper<>();

        if (batchId != null) {
            wrapper.eq(TraceProcessing::getBatchId, batchId);
        }
        if (enterpriseId != null) {
            wrapper.eq(TraceProcessing::getProcessingEnterpriseId, enterpriseId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w
                    .like(TraceProcessing::getSourceBatchCode, kw)
                    .or()
                    .like(TraceProcessing::getProcessMethod, kw)
                    .or()
                    .like(TraceProcessing::getOperator, kw)
            );
        }

        wrapper.orderByDesc(TraceProcessing::getCreateTime);
        Page<TraceProcessing> resultPage = traceProcessingMapper.selectPage(page, wrapper);

        // Enrich with batch info (batchCode, productName, enterpriseName)
        Page<Map<String, Object>> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        if (resultPage.getRecords() != null && !resultPage.getRecords().isEmpty()) {
            // Collect batchIds for batch lookup
            List<Long> batchIds = resultPage.getRecords().stream()
                    .map(TraceProcessing::getBatchId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, TraceBatch> batchMap = new HashMap<>();
            if (!batchIds.isEmpty()) {
                List<TraceBatch> batches = traceBatchMapper.selectBatchIds(batchIds);
                batchMap = batches.stream().collect(Collectors.toMap(TraceBatch::getId, b -> b));
            }

            Map<Long, TraceBatch> finalBatchMap = batchMap;
            List<Map<String, Object>> voList = resultPage.getRecords().stream().map(r -> {
                Map<String, Object> vo = new HashMap<>();
                vo.put("id", r.getId());
                vo.put("batchId", r.getBatchId());
                vo.put("sourceBatchCode", r.getSourceBatchCode());
                vo.put("processingEnterpriseId", r.getProcessingEnterpriseId());
                vo.put("processingDate", r.getProcessingDate());
                vo.put("processMethod", r.getProcessMethod());
                vo.put("specs", r.getSpecs());
                vo.put("operator", r.getOperator());
                vo.put("inputQuantity", r.getInputQuantity());
                vo.put("inputUnit", r.getInputUnit());
                vo.put("outputQuantity", r.getOutputQuantity());
                vo.put("outputUnit", r.getOutputUnit());
                vo.put("images", r.getImages());
                vo.put("txHash", r.getTxHash());
                vo.put("blockNumber", r.getBlockNumber());
                vo.put("chainTime", r.getChainTime());
                vo.put("dataHash", r.getDataHash());
                vo.put("remark", r.getRemark());
                vo.put("createTime", r.getCreateTime());

                TraceBatch b = finalBatchMap.get(r.getBatchId());
                if (b != null) {
                    vo.put("batchCode", b.getBatchCode());
                    vo.put("productName", b.getProductName());
                    vo.put("breed", b.getBreed());
                    vo.put("unit", b.getUnit());
                    vo.put("batchStatus", b.getBatchStatus());
                }
                return vo;
            }).collect(Collectors.toList());
            voPage.setRecords(voList);
        }
        return voPage;
    }

    /**
     * Find batch by batchCode
     */
    private TraceBatch findBatchByCode(String batchCode) {
        LambdaQueryWrapper<TraceBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TraceBatch::getBatchCode, batchCode.trim());
        TraceBatch batch = traceBatchMapper.selectOne(wrapper);
        if (batch == null) {
            throw new RuntimeException("batch not found for batchCode: " + batchCode);
        }
        return batch;
    }

    private String computeSHA256(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("SHA-256计算失败", e);
            return "hash_error_" + System.currentTimeMillis();
        }
    }

    /**
     * 删除加工记录，同时回滚批次状态
     * 加工记录删除后：
     *   - 若该批次无其他加工记录 → 状态回到4（加工中/已接收）
     *   - 若该批次还有其他加工记录 → 保持当前状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessing(Long id) {
        TraceProcessing processing = traceProcessingMapper.selectById(id);
        if (processing == null) {
            throw new RuntimeException("加工记录不存在");
        }

        Long batchId = processing.getBatchId();
        traceProcessingMapper.deleteById(id);
        log.info("加工记录已删除, id={}, batchId={}", id, batchId);

        // 回滚批次状态
        if (batchId != null) {
            TraceBatch batch = traceBatchMapper.selectById(batchId);
            if (batch != null && batch.getBatchStatus() != null && batch.getBatchStatus() == 9) {
                // 检查该批次是否还有其他加工记录
                LambdaQueryWrapper<TraceProcessing> otherProcWrapper = new LambdaQueryWrapper<>();
                otherProcWrapper.eq(TraceProcessing::getBatchId, batchId);
                Long remainingProc = traceProcessingMapper.selectCount(otherProcWrapper);

                if (remainingProc == 0) {
                    // 无其他加工记录，回滚到4（加工中/已接收）
                    batch.setBatchStatus(4);
                    batch.setUpdateTime(LocalDateTime.now());
                    traceBatchMapper.updateById(batch);
                    log.info("批次状态回滚: batchId={}, {} -> {}",
                            batchId, BatchService.statusName(5), BatchService.statusName(4));
                }
            }
        }
    }
}
