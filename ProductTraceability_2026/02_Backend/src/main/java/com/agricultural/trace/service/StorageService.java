package com.agricultural.trace.service;

import com.agricultural.trace.entity.*;
import com.agricultural.trace.mapper.*;
import com.agricultural.trace.utils.Web3jUtils;
import com.agricultural.trace.mapper.EnterpriseInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仓储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final TraceStorageMapper traceStorageMapper;
    private final TraceBatchMapper traceBatchMapper;
    private final TraceRecordMapper traceRecordMapper;
    private final TraceInspectionMapper traceInspectionMapper;
    private final TraceProcessingMapper traceProcessingMapper;
    private final TraceTransportMapper traceTransportMapper;
    private final Web3jUtils web3jUtils;
    private final BlockchainTransactionService blockchainTransactionService;
    private final MessageService messageService;
    private final EnterpriseInfoMapper enterpriseInfoMapper;

    /**
     * 分页查询仓储记录（包含批次和企业信息）
     */
    public Page<java.util.Map<String, Object>> getStorageList(Long current, Long size,
                                              Long batchId, Long enterpriseId,
                                              Integer storageType, String keyword) {
        Page<TraceStorage> page = new Page<>(current, size);
        LambdaQueryWrapper<TraceStorage> wrapper = new LambdaQueryWrapper<>();

        if (batchId != null) {
            wrapper.eq(TraceStorage::getBatchId, batchId);
        }
        if (enterpriseId != null) {
            wrapper.eq(TraceStorage::getStorageEnterpriseId, enterpriseId);
        }
        if (storageType != null) {
            wrapper.eq(TraceStorage::getStorageType, storageType);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w
                .like(TraceStorage::getBatchCode, kw)
                .or().like(TraceStorage::getWarehouseName, kw)
                .or().like(TraceStorage::getOperator, kw)
            );
        }
        wrapper.orderByDesc(TraceStorage::getStorageDate);
        wrapper.orderByDesc(TraceStorage::getCreateTime);

        Page<TraceStorage> storagePage = traceStorageMapper.selectPage(page, wrapper);
        
        Page<java.util.Map<String, Object>> resultPage = new Page<>(current, size);
        resultPage.setTotal(storagePage.getTotal());
        resultPage.setPages(storagePage.getPages());
        
        java.util.List<java.util.Map<String, Object>> records = new java.util.ArrayList<>();
        for (TraceStorage storage : storagePage.getRecords()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", storage.getId());
            map.put("batchId", storage.getBatchId());
            map.put("batchCode", storage.getBatchCode());
            map.put("storageType", storage.getStorageType());
            map.put("storageDate", storage.getStorageDate());
            map.put("storageQuantity", storage.getStorageQuantity());
            map.put("storageUnit", storage.getStorageUnit());
            map.put("warehouseName", storage.getWarehouseName());
            map.put("warehouseLocation", storage.getWarehouseLocation());
            map.put("storageCondition", storage.getStorageCondition());
            map.put("operator", storage.getOperator());
            map.put("temperature", storage.getTemperature());
            map.put("humidity", storage.getHumidity());
            map.put("images", storage.getImages());
            map.put("dataHash", storage.getDataHash());
            map.put("remark", storage.getRemark());
            map.put("createTime", storage.getCreateTime());
            map.put("updateTime", storage.getUpdateTime());
            map.put("txHash", storage.getTxHash());
            map.put("blockNumber", storage.getBlockNumber());
            map.put("chainTime", storage.getChainTime());
            
            if (storage.getBatchId() != null) {
                TraceBatch batch = traceBatchMapper.selectById(storage.getBatchId());
                if (batch != null) {
                    map.put("productName", batch.getProductName());
                    map.put("breed", batch.getBreed());
                    map.put("unit", batch.getUnit());
                    map.put("quarantinePassed", hasPassedInspection(batch.getId()));
                    
                    if (batch.getEnterpriseId() != null) {
                        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(batch.getEnterpriseId());
                        if (enterprise != null) {
                            map.put("enterpriseName", enterprise.getEnterpriseName());
                        }
                    }
                }
            }
            
            records.add(map);
        }
        resultPage.setRecords(records);
        
        return resultPage;
    }

    /**
     * 获取批次可操作数量信息
     * totalBatch: 批次总量
     * totalInbound: 已入库总量 (storage_type=1)
     * totalOutbound: 已出库总量 (storage_type=2)
     * availableInbound: 可入库数量 = 批次总量 - 已入库
     * availableOutbound: 可出库数量 = 已入库 - 已出库
     * currentStock: 当前库存 = 已入库 - 已出库
     * unit: 单位
     */
    public Map<String, Object> getAvailableQuantity(Long batchId, Long enterpriseId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在");
        }

        BigDecimal totalBatch = batch.getInitQuantity() != null
                ? batch.getInitQuantity() : BigDecimal.ZERO;

        LambdaQueryWrapper<TraceStorage> inWrapper = new LambdaQueryWrapper<>();
        inWrapper.eq(TraceStorage::getBatchId, batchId);
        inWrapper.eq(TraceStorage::getStorageType, 1);
        if (enterpriseId != null) {
            inWrapper.eq(TraceStorage::getStorageEnterpriseId, enterpriseId);
        }
        List<TraceStorage> inboundList = traceStorageMapper.selectList(inWrapper);
        BigDecimal totalInbound = inboundList.stream()
                .filter(s -> s.getStorageQuantity() != null)
                .map(TraceStorage::getStorageQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LambdaQueryWrapper<TraceStorage> outWrapper = new LambdaQueryWrapper<>();
        outWrapper.eq(TraceStorage::getBatchId, batchId);
        outWrapper.eq(TraceStorage::getStorageType, 2);
        if (enterpriseId != null) {
            outWrapper.eq(TraceStorage::getStorageEnterpriseId, enterpriseId);
        }
        List<TraceStorage> outboundList = traceStorageMapper.selectList(outWrapper);
        BigDecimal totalOutbound = outboundList.stream()
                .filter(s -> s.getStorageQuantity() != null)
                .map(TraceStorage::getStorageQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableInbound = totalBatch.subtract(totalInbound).max(BigDecimal.ZERO);
        BigDecimal availableOutbound = totalInbound.subtract(totalOutbound).max(BigDecimal.ZERO);
        BigDecimal currentStock = availableOutbound;

        Map<String, Object> result = new HashMap<>();
        result.put("totalBatch", totalBatch);
        result.put("totalInbound", totalInbound);
        result.put("totalOutbound", totalOutbound);
        result.put("availableInbound", availableInbound);
        result.put("availableOutbound", availableOutbound);
        result.put("currentStock", currentStock);
        result.put("unit", batch.getUnit() != null ? batch.getUnit() : "");
        return result;
    }

    /**
     * 新增仓储记录 (supports batchCode -> batchId resolution)
     * storageUnit: auto-fill from batch.unit if not provided
     * 提交时打包该批次全部数据（批次+生长记录+检疫记录+仓储记录）上链
     */
    @Transactional(rollbackFor = Exception.class)
    public TraceStorage createStorage(TraceStorage record) {
        // Resolve batchCode -> batchId if batchId is not provided
        TraceBatch batch = null;
        if (record.getBatchId() == null && StringUtils.hasText(record.getBatchCode())) {
            batch = findBatchByCode(record.getBatchCode());
            record.setBatchId(batch.getId());
        } else if (record.getBatchId() != null) {
            batch = traceBatchMapper.selectById(record.getBatchId());
        }

        if (batch != null && !hasPassedInspection(batch.getId())) {
            throw new RuntimeException("该批次未取得检疫合格记录，禁止继续入库");
        }

        // 数量上限校验
        if (batch != null && record.getStorageQuantity() != null && record.getStorageType() != null) {
            Map<String, Object> available = getAvailableQuantity(batch.getId(), record.getStorageEnterpriseId());
            BigDecimal qty = record.getStorageQuantity();
            int type = record.getStorageType();
            if (type == 1) {
                BigDecimal limit = (BigDecimal) available.get("availableInbound");
                if (limit.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("该批次已达入库上限，无法继续入库");
                }
                if (qty.compareTo(limit) > 0) {
                    throw new RuntimeException("入库数量不能超过可入库上限 " + limit.stripTrailingZeros().toPlainString() + " " + available.get("unit"));
                }
            } else if (type == 2) {
                BigDecimal limit = (BigDecimal) available.get("availableOutbound");
                if (limit.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("当前库存为零，无法出库");
                }
                if (qty.compareTo(limit) > 0) {
                    throw new RuntimeException("出库数量不能超过可出库上限 " + limit.stripTrailingZeros().toPlainString() + " " + available.get("unit"));
                }
            } else if (type == 3) {
                BigDecimal limit = (BigDecimal) available.get("currentStock");
                if (qty.compareTo(limit) > 0) {
                    throw new RuntimeException("盘点数量不能超过当前库存 " + limit.stripTrailingZeros().toPlainString() + " " + available.get("unit"));
                }
            }
        }

        // Auto-fill storageUnit from batch.unit
        if (!StringUtils.hasText(record.getStorageUnit()) && batch != null && StringUtils.hasText(batch.getUnit())) {
            record.setStorageUnit(batch.getUnit());
        }

        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        traceStorageMapper.insert(record);
        log.info("新增仓储记录, batchCode={}, batchId={}, type={}, warehouse={}, storageUnit={}",
                record.getBatchCode(), record.getBatchId(), record.getStorageType(),
                record.getWarehouseName(), record.getStorageUnit());

        // 如果是入库操作，判断是否由加工企业执行
        // 只有加工企业（storageEnterpriseId != batch.enterpriseId）入库才更新批次状态为6（已入库）
        // 养殖企业自身入库仅做仓储记录，不改变批次流转状态
        if (record.getStorageType() != null && record.getStorageType() == 1 && batch != null) {
            Long storageEntId = record.getStorageEnterpriseId();
            Long batchEntId = batch.getEnterpriseId();
            if (storageEntId != null && batchEntId != null && !storageEntId.equals(batchEntId)) {
                batch.setBatchStatus(6);
                batch.setUpdateTime(LocalDateTime.now());
                traceBatchMapper.updateById(batch);
                log.info("加工企业入库，批次状态更新为6（已入库）, batchCode={}, storageEnterpriseId={}", batch.getBatchCode(), storageEntId);
            } else {
                log.info("养殖企业自身入库，不改变批次状态, batchCode={}, storageEnterpriseId={}", batch.getBatchCode(), storageEntId);
            }
        }

        // ====== 打包该批次全部数据上链 ======
        if (batch != null) {
            try {
                String dataHash = buildFullBatchHash(batch, record);
                String traceId = batch.getBatchCode() + "_STORAGE_" + record.getId();
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

                // 回写仓储记录的上链信息
                record.setTxHash(txHash);
                record.setDataHash(dataHash);
                record.setBlockNumber(blockNumber);
                record.setChainTime(chainTime);
                record.setUpdateTime(LocalDateTime.now());
                traceStorageMapper.updateById(record);

                log.info("仓储记录上链成功, batchCode={}, txHash={}, blockNumber={}",
                        batch.getBatchCode(), txHash, blockNumber);

                // 记录交易到 blockchain_transaction 表
                org.web3j.protocol.core.methods.response.TransactionReceipt fullReceipt =
                        web3jUtils.getTransactionReceipt(txHash);
                blockchainTransactionService.recordTransaction(txHash,
                        BlockchainTransactionService.BIZ_STORAGE, record.getId(),
                        batch.getId(), dataHash, fullReceipt);

                // 发送上链成功系统通知给企业全员
                try {
                    String enterpriseName = "";
                    EnterpriseInfo ent = enterpriseInfoMapper.selectById(batch.getEnterpriseId());
                    if (ent != null) {
                        enterpriseName = ent.getEnterpriseName();
                    }
                    String operatorName = record.getOperator() != null ? record.getOperator() : "系统";
                    String msgTitle = "【上链成功】入库记录已上链";
                    String msgSummary = operatorName + "对批次 " + batch.getBatchCode() + " 的入库记录已成功上链";
                    String msgContent = "上链成功通知\n\n"
                            + "企业名称：" + enterpriseName + "\n"
                            + "操作人员：" + operatorName + "\n"
                            + "批次编号：" + batch.getBatchCode() + "\n"
                            + "产品名称：" + batch.getProductName() + "\n"
                            + "交易哈希：" + txHash + "\n"
                            + "区块高度：" + blockNumber + "\n"
                            + "上链时间：" + chainTime;
                    messageService.sendToEnterprise(batch.getEnterpriseId(), "system", msgTitle, msgSummary, msgContent,
                            null, null, null);
                } catch (Exception msgEx) {
                    log.warn("发送上链通知失败(不影响业务): {}", msgEx.getMessage());
                }
            } catch (Exception e) {
                log.warn("仓储记录上链失败(不影响业务): {}", e.getMessage());
            }
        }

        return record;
    }

    /**
     * 打包批次全部数据生成哈希
     * 包含：批次信息 + 所有生长记录 + 所有检疫记录 + 当前仓储记录
     */
    private String buildFullBatchHash(TraceBatch batch, TraceStorage currentStorage) {
        StringBuilder sb = new StringBuilder();

        // 1. 批次基本信息
        sb.append("BATCH|");
        sb.append("id=").append(batch.getId()).append("|");
        sb.append("code=").append(batch.getBatchCode()).append("|");
        sb.append("product=").append(batch.getProductName()).append("|");
        sb.append("type=").append(batch.getProductType()).append("|");
        sb.append("breed=").append(batch.getBreed()).append("|");
        sb.append("qty=").append(batch.getInitQuantity()).append("|");
        sb.append("origin=").append(batch.getOriginLocation()).append("|");
        sb.append("enterprise=").append(batch.getEnterpriseId()).append("|");

        // 2. 所有生长记录
        List<TraceRecord> records = traceRecordMapper.selectList(
                new LambdaQueryWrapper<TraceRecord>()
                        .eq(TraceRecord::getBatchId, batch.getId())
                        .orderByAsc(TraceRecord::getRecordDate));
        sb.append("RECORDS[").append(records.size()).append("]|");
        for (TraceRecord r : records) {
            sb.append("R{type=").append(r.getRecordType())
              .append(",date=").append(r.getRecordDate())
              .append(",item=").append(r.getItemName())
              .append(",amount=").append(r.getAmount())
              .append(",desc=").append(r.getDescription())
              .append(",op=").append(r.getOperator())
              .append("}|");
        }

        // 3. 所有检疫记录
        List<TraceInspection> inspections = traceInspectionMapper.selectList(
                new LambdaQueryWrapper<TraceInspection>()
                        .eq(TraceInspection::getBatchId, batch.getId())
                        .orderByAsc(TraceInspection::getInspectionDate));
        sb.append("INSPECTIONS[").append(inspections.size()).append("]|");
        for (TraceInspection ins : inspections) {
            sb.append("I{date=").append(ins.getInspectionDate())
              .append(",result=").append(ins.getCheckResult())
              .append(",certNo=").append(ins.getCertNo())
              .append(",inspector=").append(ins.getInspector())
              .append("}|");
        }

        // 4. 当前仓储记录
        sb.append("STORAGE{type=").append(currentStorage.getStorageType())
          .append(",date=").append(currentStorage.getStorageDate())
          .append(",warehouse=").append(currentStorage.getWarehouseName())
          .append(",qty=").append(currentStorage.getStorageQuantity())
          .append(",op=").append(currentStorage.getOperator())
          .append("}|");

        // 5. 时间戳
        sb.append("ts=").append(System.currentTimeMillis());

        return computeSHA256(sb.toString());
    }

    private String computeSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
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
     * Find batch entity by batchCode
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

    private boolean hasPassedInspection(Long batchId) {
        if (batchId == null) {
            return false;
        }
        LambdaQueryWrapper<TraceInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TraceInspection::getBatchId, batchId);
        wrapper.eq(TraceInspection::getCheckResult, 1);
        return traceInspectionMapper.selectCount(wrapper) > 0;
    }

    /**
     * 删除仓储记录，同时回滚批次状态
     * 仅加工企业（storageEnterpriseId != batch.enterpriseId）的入库记录删除才触发回滚：
     *   - 若该批次无其他加工企业入库记录且状态为6 → 有加工记录回到9（加工完成），否则回到4（加工中）
     * 养殖企业自身入库记录删除不影响批次状态
     */
    /**
     * 更新仓储记录
     */
    public void updateStorage(TraceStorage record) {
        if (record.getStorageEnterpriseId() == null && record.getEnterpriseId() != null) {
            record.setStorageEnterpriseId(record.getEnterpriseId());
        }
        record.setUpdateTime(java.time.LocalDateTime.now());
        traceStorageMapper.updateById(record);
        log.info("更新仓储记录, id={}, batchId={}", record.getId(), record.getBatchId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteStorage(Long id) {
        TraceStorage storage = traceStorageMapper.selectById(id);
        if (storage == null) {
            throw new RuntimeException("仓储记录不存在");
        }

        Long batchId = storage.getBatchId();
        Integer storageType = storage.getStorageType();
        Long storageEntId = storage.getStorageEnterpriseId();

        traceStorageMapper.deleteById(id);
        log.info("删除仓储记录, id={}, batchId={}, storageType={}, storageEnterpriseId={}", id, batchId, storageType, storageEntId);

        // 仅对入库记录做回滚判断
        if (batchId != null && storageType != null && storageType == 1) {
            TraceBatch batch = traceBatchMapper.selectById(batchId);
            if (batch == null) return;

            // 只有加工企业（非批次创建企业）的入库记录删除才触发状态回滚
            Long batchEntId = batch.getEnterpriseId();
            boolean isProcessingEnterprise = storageEntId != null && batchEntId != null && !storageEntId.equals(batchEntId);

            if (isProcessingEnterprise && batch.getBatchStatus() != null && batch.getBatchStatus() == 6) {
                // 检查该批次是否还有其他加工企业的入库记录
                LambdaQueryWrapper<TraceStorage> otherStorageWrapper = new LambdaQueryWrapper<>();
                otherStorageWrapper.eq(TraceStorage::getBatchId, batchId);
                otherStorageWrapper.eq(TraceStorage::getStorageType, 1);
                otherStorageWrapper.ne(TraceStorage::getStorageEnterpriseId, batchEntId);
                Long remainingInbound = traceStorageMapper.selectCount(otherStorageWrapper);

                if (remainingInbound == 0) {
                    // 无其他加工企业入库记录，回滚状态
                    LambdaQueryWrapper<com.agricultural.trace.entity.TraceProcessing> procWrapper = new LambdaQueryWrapper<>();
                    procWrapper.eq(com.agricultural.trace.entity.TraceProcessing::getBatchId, batchId);
                    Long procCount = traceProcessingMapper.selectCount(procWrapper);

                    int newStatus = procCount > 0 ? 9 : 4;
                    batch.setBatchStatus(newStatus);
                    batch.setUpdateTime(java.time.LocalDateTime.now());
                    traceBatchMapper.updateById(batch);
                    log.info("批次状态回滚: batchId={}, {} -> {}", batchId,
                            BatchService.statusName(6), BatchService.statusName(newStatus));
                }
            } else if (!isProcessingEnterprise) {
                log.info("养殖企业入库记录删除，不回滚批次状态, batchId={}", batchId);
            }
        }
    }
}
