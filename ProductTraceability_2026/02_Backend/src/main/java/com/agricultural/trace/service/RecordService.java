package com.agricultural.trace.service;

import com.agricultural.trace.entity.TraceBatch;
import com.agricultural.trace.entity.TraceRecord;
import com.agricultural.trace.mapper.TraceBatchMapper;
import com.agricultural.trace.mapper.TraceRecordMapper;
import com.agricultural.trace.utils.Web3jUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生长记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {

    private final TraceRecordMapper traceRecordMapper;
    private final TraceBatchMapper traceBatchMapper;
    private final Web3jUtils web3jUtils;
    private final BlockchainTransactionService blockchainTransactionService;

    /**
     * 分页查询生长记录
     */
    public Page<TraceRecord> getRecordList(Long pageNum, Long pageSize,
                                           Long batchId, String recordType) {
        Page<TraceRecord> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<TraceRecord> wrapper = new LambdaQueryWrapper<>();

        if (batchId != null) {
            wrapper.eq(TraceRecord::getBatchId, batchId);
        }

        if (StringUtils.hasText(recordType)) {
            wrapper.eq(TraceRecord::getRecordType, recordType.trim());
        }

        wrapper.orderByDesc(TraceRecord::getRecordDate);
        wrapper.orderByDesc(TraceRecord::getCreateTime);

        return traceRecordMapper.selectPage(page, wrapper);
    }

    /**
     * 新增生长记录 (supports batchCode -> batchId resolution)
     */
    @Transactional(rollbackFor = Exception.class)
    public TraceRecord createRecordWithBatchCode(TraceRecord record) {
        TraceBatch batch = null;
        if (record.getBatchId() == null && StringUtils.hasText(record.getBatchCode())) {
            batch = findBatchByCode(record.getBatchCode());
            record.setBatchId(batch.getId());
        } else if (record.getBatchId() != null) {
            batch = traceBatchMapper.selectById(record.getBatchId());
        }
        if (record.getBatchId() == null) {
            throw new RuntimeException("batch_id is required (provide batchCode or batchId)");
        }

        prepareRecordForSave(record, batch);
        traceRecordMapper.insert(record);
        promoteBatchToGrowing(batch, record.getBatchId());

        log.info("新增生长记录, batchId={}, batchCode={}, type={}, item={}",
                record.getBatchId(), record.getBatchCode(), record.getRecordType(), record.getItemName());

        chainUploadRecord(record);
        return record;
    }

    /**
     * 新增生长记录 (legacy, uses batchId directly)
     */
    @Transactional(rollbackFor = Exception.class)
    public TraceRecord createRecord(TraceRecord record) {
        TraceBatch batch = null;
        if (record.getBatchId() != null) {
            batch = traceBatchMapper.selectById(record.getBatchId());
        }

        prepareRecordForSave(record, batch);
        traceRecordMapper.insert(record);
        promoteBatchToGrowing(batch, record.getBatchId());

        log.info("新增生长记录, batchId={}, type={}, item={}",
                record.getBatchId(), record.getRecordType(), record.getItemName());

        chainUploadRecord(record);
        return record;
    }

    /**
     * Resolve batchCode to batchId by querying trace_batch table
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

    private void prepareRecordForSave(TraceRecord record, TraceBatch batch) {
        validateRecordDate(record);
        if (!StringUtils.hasText(record.getBatchCode()) && batch != null) {
            record.setBatchCode(batch.getBatchCode());
        }
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
    }

    private void validateRecordDate(TraceRecord record) {
        LocalDate recordDate = record.getRecordDate();
        if (recordDate != null && recordDate.isAfter(LocalDate.now())) {
            throw new RuntimeException("记录日期不能晚于今天");
        }
    }

    private void promoteBatchToGrowing(TraceBatch batch, Long batchId) {
        TraceBatch targetBatch = batch;
        if (targetBatch == null && batchId != null) {
            targetBatch = traceBatchMapper.selectById(batchId);
        }
        if (targetBatch == null) {
            return;
        }
        if (Integer.valueOf(1).equals(targetBatch.getBatchStatus())) {
            targetBatch.setBatchStatus(2);
            targetBatch.setUpdateTime(LocalDateTime.now());
            traceBatchMapper.updateById(targetBatch);
        }
    }

    /**
     * 软删除生长记录（标记deleted=1, 记录deleteTime）
     * 物理文件由定时任务在7天后清理
     */
    public void deleteRecord(Long id) {
        TraceRecord record = traceRecordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("记录不存在 id=" + id);
        }
        traceRecordMapper.softDeleteById(id);
        log.info("软删除生长记录 id={}, images={}", id, record.getImages());
    }

    /**
     * 查询已软删除且超过指定天数的记录（供定时任务使用，绕过TableLogic）
     */
    public java.util.List<TraceRecord> findDeletedRecordsOlderThan(int days) {
        return traceRecordMapper.selectDeletedOlderThan(days);
    }

    /**
     * 物理删除记录（真正从数据库中移除，绕过TableLogic）
     */
    public void physicalDelete(Long id) {
        traceRecordMapper.physicalDeleteById(id);
        log.info("物理删除生长记录, id={}", id);
    }

    private void chainUploadRecord(TraceRecord record) {
        try {
            String dataHash = computeSHA256("RECORD|batchId=" + record.getBatchId()
                    + "|type=" + record.getRecordType()
                    + "|item=" + record.getItemName()
                    + "|date=" + record.getRecordDate()
                    + "|id=" + record.getId()
                    + "|ts=" + System.currentTimeMillis());
            String traceId = record.getBatchCode() + "_RECORD_" + record.getId();
            String txHash = web3jUtils.uploadHash(traceId, dataHash);

            Long blockNumber = null;
            try {
                org.web3j.protocol.core.methods.response.TransactionReceipt receipt =
                        web3jUtils.getTransactionReceipt(txHash);
                if (receipt != null) {
                    blockNumber = receipt.getBlockNumber().longValue();
                }
            } catch (Exception ignored) {
            }

            LocalDateTime chainTime = LocalDateTime.now();
            record.setTxHash(txHash);
            record.setDataHash(dataHash);
            record.setBlockNumber(blockNumber);
            record.setChainTime(chainTime);
            record.setUpdateTime(LocalDateTime.now());
            traceRecordMapper.updateById(record);

            log.info("生长记录上链成功, id={}, txHash={}, blockNumber={}", record.getId(), txHash, blockNumber);

            // 记录交易到 blockchain_transaction 表
            org.web3j.protocol.core.methods.response.TransactionReceipt fullReceipt =
                    web3jUtils.getTransactionReceipt(txHash);
            blockchainTransactionService.recordTransaction(txHash,
                    BlockchainTransactionService.BIZ_RECORD, record.getId(),
                    record.getBatchId(), dataHash, fullReceipt);
        } catch (Exception e) {
            log.warn("生长记录上链失败(不影响业务): {}", e.getMessage());
        }
    }

    private String computeSHA256(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("SHA-256计算失败", e);
            return "hash_error_" + System.currentTimeMillis();
        }
    }
}
