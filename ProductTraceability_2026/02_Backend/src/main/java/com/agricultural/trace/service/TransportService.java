package com.agricultural.trace.service;

import com.agricultural.trace.entity.TraceBatch;
import com.agricultural.trace.entity.TraceInspection;
import com.agricultural.trace.entity.TraceStorage;
import com.agricultural.trace.entity.TraceTransport;
import com.agricultural.trace.mapper.TraceBatchMapper;
import com.agricultural.trace.mapper.TraceInspectionMapper;
import com.agricultural.trace.mapper.TraceStorageMapper;
import com.agricultural.trace.mapper.TraceTransportMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运输服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransportService {

    private static final int MAX_PLATE_NUMBER_LENGTH = 20;

    private final TraceTransportMapper traceTransportMapper;
    private final TraceBatchMapper traceBatchMapper;
    private final TraceInspectionMapper traceInspectionMapper;
    private final TraceStorageMapper traceStorageMapper;
    private final com.agricultural.trace.utils.Web3jUtils web3jUtils;
    private final BlockchainTransactionService blockchainTransactionService;

    /**
     * 获取批次可运输数量信息
     * totalOutbound: 已出库总量
     * totalTransported: 已运输总量
     * availableTransport: 可运输数量 = 已出库 - 已运输
     * unit: 单位
     */
    public Map<String, Object> getAvailableTransportQuantity(Long batchId, Long enterpriseId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在");
        }

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

        LambdaQueryWrapper<TraceTransport> transportWrapper = new LambdaQueryWrapper<>();
        transportWrapper.eq(TraceTransport::getBatchId, batchId);
        if (enterpriseId != null) {
            transportWrapper.eq(TraceTransport::getTransportEnterpriseId, enterpriseId);
        }
        List<TraceTransport> transportList = traceTransportMapper.selectList(transportWrapper);
        BigDecimal totalTransported = transportList.stream()
                .filter(t -> t.getTransportQuantity() != null)
                .map(TraceTransport::getTransportQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableTransport = totalOutbound.subtract(totalTransported).max(BigDecimal.ZERO);

        Map<String, Object> result = new HashMap<>();
        result.put("totalOutbound", totalOutbound);
        result.put("totalTransported", totalTransported);
        result.put("availableTransport", availableTransport);
        result.put("unit", batch.getUnit() != null ? batch.getUnit() : "");
        return result;
    }

    /**
     * 创建运输记录
     * 支持通过 batchCode 自动解析 batchId
     * Auto-fill: departureTime (current time), transportUnit (from batch)
     */
    public TraceTransport createTransport(TraceTransport record) {
        normalizeAndValidatePlateNumber(record);

        // Resolve batchCode -> batchId if batchId is not provided
        TraceBatch batch = null;
        if (record.getBatchId() == null && StringUtils.hasText(record.getBatchCode())) {
            batch = findBatchByCode(record.getBatchCode());
            record.setBatchId(batch.getId());
        } else if (record.getBatchId() != null) {
            batch = traceBatchMapper.selectById(record.getBatchId());
        }

        if (batch != null && !hasPassedInspection(batch.getId())) {
            throw new RuntimeException("该批次未取得检疫合格记录，禁止继续运输");
        }

        // 运输数量上限校验
        if (batch != null && record.getTransportQuantity() != null) {
            Long entId = record.getTransportEnterpriseId() != null ? record.getTransportEnterpriseId() : record.getEnterpriseId();
            Map<String, Object> available = getAvailableTransportQuantity(batch.getId(), entId);
            BigDecimal limit = (BigDecimal) available.get("availableTransport");
            BigDecimal qty = record.getTransportQuantity();
            if (limit.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("该批次无可运输数量，请先完成出库操作");
            }
            if (qty.compareTo(limit) > 0) {
                throw new RuntimeException("运输数量不能超过可运输上限 " + limit.stripTrailingZeros().toPlainString() + " " + available.get("unit"));
            }
        }

        // Auto-fill departureTime as current time
        if (record.getDepartureTime() == null) {
            record.setDepartureTime(LocalDateTime.now());
        }

        // Auto-fill transportUnit from batch.unit
        if (!StringUtils.hasText(record.getTransportUnit()) && batch != null && StringUtils.hasText(batch.getUnit())) {
            record.setTransportUnit(batch.getUnit());
        }

        // 运输记录需独立上链，不继承批次的上链信息

        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        traceTransportMapper.insert(record);

        // 更新批次状态为运输中(7)，但若已销售(8)则不回退状态
        if (batch != null && (batch.getBatchStatus() == null || batch.getBatchStatus() < 8)) {
            batch.setBatchStatus(7);
            batch.setUpdateTime(LocalDateTime.now());
            traceBatchMapper.updateById(batch);
            log.info("批次状态更新为运输中(7), batchCode={}", batch.getBatchCode());
        }

        log.info("新增运输记录, batchCode={}, batchId={}, logisticsNo={}, departureTime={}",
                record.getBatchCode(), record.getBatchId(), record.getLogisticsNo(), record.getDepartureTime());

        // ====== 运输记录独立上链 ======
        try {
            String dataHash = computeSHA256("TRANSPORT|batchId=" + record.getBatchId()
                    + "|plate=" + record.getPlateNumber()
                    + "|driver=" + record.getDriverName()
                    + "|from=" + record.getDepartureLocation()
                    + "|to=" + record.getDestination()
                    + "|id=" + record.getId()
                    + "|ts=" + System.currentTimeMillis());
            String traceId = record.getBatchCode() + "_TRANSPORT_" + record.getId();
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
            traceTransportMapper.updateById(record);

            log.info("运输记录上链成功, id={}, txHash={}, blockNumber={}", record.getId(), txHash, blockNumber);

            // 记录交易到 blockchain_transaction 表
            org.web3j.protocol.core.methods.response.TransactionReceipt fullReceipt =
                    web3jUtils.getTransactionReceipt(txHash);
            blockchainTransactionService.recordTransaction(txHash,
                    BlockchainTransactionService.BIZ_TRANSPORT, record.getId(),
                    record.getBatchId(), dataHash, fullReceipt);
        } catch (Exception e) {
            log.warn("运输记录上链失败(不影响业务): {}", e.getMessage());
        }

        return record;
    }

    /**
     * 分页查询运输记录列表
     */
    public Page<TraceTransport> getTransportList(Long pageNum, Long pageSize, Long batchId, Long enterpriseId, Long receiveEnterpriseId, String keyword) {
        Page<TraceTransport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TraceTransport> wrapper = new LambdaQueryWrapper<>();

        if (batchId != null) {
            wrapper.eq(TraceTransport::getBatchId, batchId);
        }
        if (enterpriseId != null) {
            wrapper.eq(TraceTransport::getTransportEnterpriseId, enterpriseId);
        }
        if (receiveEnterpriseId != null) {
            wrapper.eq(TraceTransport::getReceiveEnterpriseId, receiveEnterpriseId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w
                .like(TraceTransport::getBatchCode, kw)
                .or().like(TraceTransport::getLogisticsNo, kw)
                .or().like(TraceTransport::getDriverName, kw)
            );
        }

        wrapper.orderByDesc(TraceTransport::getTransportDate);
        wrapper.orderByDesc(TraceTransport::getCreateTime);

        return traceTransportMapper.selectPage(page, wrapper);
    }

    /**
     * 更新运输记录
     */
    public void updateTransport(TraceTransport record) {
        normalizeAndValidatePlateNumber(record);
        if (record.getTransportEnterpriseId() == null && record.getEnterpriseId() != null) {
            record.setTransportEnterpriseId(record.getEnterpriseId());
        }
        record.setUpdateTime(LocalDateTime.now());
        traceTransportMapper.updateById(record);
        log.info("更新运输记录, id={}, batchId={}", record.getId(), record.getBatchId());
    }

    /**
     * 删除运输记录，同时回滚批次状态
     * 运输记录删除后：
     *   - 若该批次无其他运输记录且当前状态为7(运输中) → 回滚到6(已入库)
     */
    public void deleteTransport(Long id) {
        TraceTransport transport = traceTransportMapper.selectById(id);
        if (transport == null) {
            throw new RuntimeException("运输记录不存在");
        }

        Long batchId = transport.getBatchId();
        traceTransportMapper.deleteById(id);
        log.info("删除运输记录, id={}, batchId={}", id, batchId);

        // 回滚批次状态
        if (batchId != null) {
            TraceBatch batch = traceBatchMapper.selectById(batchId);
            if (batch != null && batch.getBatchStatus() != null && batch.getBatchStatus() == 7) {
                LambdaQueryWrapper<TraceTransport> otherWrapper = new LambdaQueryWrapper<>();
                otherWrapper.eq(TraceTransport::getBatchId, batchId);
                Long remaining = traceTransportMapper.selectCount(otherWrapper);

                if (remaining == 0) {
                    batch.setBatchStatus(6);
                    batch.setUpdateTime(LocalDateTime.now());
                    traceBatchMapper.updateById(batch);
                    log.info("批次状态回滚: batchId={}, {} -> {}",
                            batchId, BatchService.statusName(7), BatchService.statusName(6));
                }
            }
        }
    }

    /**
     * Find batch entity by batchCode
     */
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

    private void normalizeAndValidatePlateNumber(TraceTransport record) {
        if (record == null || !StringUtils.hasText(record.getPlateNumber())) {
            return;
        }
        String normalizedPlateNumber = record.getPlateNumber().trim();
        if (normalizedPlateNumber.length() > MAX_PLATE_NUMBER_LENGTH) {
            throw new RuntimeException("车牌号不能超过20个字符");
        }
        record.setPlateNumber(normalizedPlateNumber);
    }
}
