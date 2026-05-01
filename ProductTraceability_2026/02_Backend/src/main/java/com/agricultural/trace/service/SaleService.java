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
 * 销售记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private final TraceSaleMapper traceSaleMapper;
    private final TraceBatchMapper traceBatchMapper;
    private final TraceProcessingMapper traceProcessingMapper;
    private final TraceTransportMapper traceTransportMapper;
    private final TraceStorageMapper traceStorageMapper;
    private final Web3jUtils web3jUtils;
    private final BlockchainTransactionService blockchainTransactionService;
    private final MessageService messageService;
    private final EnterpriseInfoMapper enterpriseInfoMapper;

    /**
     * 创建销售记录
     * 如果 batchId 为空但 batchCode 有值，则通过 batchCode 查询 batchId
     * 创建销售记录后，自动更新批次状态为 8（已销售）
     * 提交时打包该批次全部加工链数据（批次+加工记录+运输记录+销售记录）上链
     */
    @Transactional(rollbackFor = Exception.class)
    public TraceSale createSale(TraceSale record) {
        // 如果 batchId 为空，通过 batchCode 查询 batchId
        TraceBatch batch = null;
        if (record.getBatchId() == null && StringUtils.hasText(record.getBatchCode())) {
            batch = findBatchByCode(record.getBatchCode());
            record.setBatchId(batch.getId());
        } else if (record.getBatchId() != null) {
            batch = traceBatchMapper.selectById(record.getBatchId());
        }

        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        record.setCreateTime(now);
        record.setUpdateTime(now);

        // 插入销售记录
        traceSaleMapper.insert(record);
        log.info("创建销售记录成功, id={}, batchCode={}, buyerName={}", 
                record.getId(), record.getBatchCode(), record.getBuyerName());

        // 判断是否达到完成条件：已销售总量 >= 该加工企业运输总量
        if (batch != null) {
            Long saleEnterpriseId = record.getSaleEnterpriseId() != null ? record.getSaleEnterpriseId() : record.getEnterpriseId();

            LambdaQueryWrapper<TraceTransport> tWrapper = new LambdaQueryWrapper<>();
            tWrapper.eq(TraceTransport::getBatchId, batch.getId());
            if (saleEnterpriseId != null) {
                tWrapper.eq(TraceTransport::getTransportEnterpriseId, saleEnterpriseId);
            }
            BigDecimal totalTransported = traceTransportMapper.selectList(tWrapper).stream()
                    .filter(t -> t.getTransportQuantity() != null)
                    .map(TraceTransport::getTransportQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            LambdaQueryWrapper<TraceSale> sWrapper = new LambdaQueryWrapper<>();
            sWrapper.eq(TraceSale::getBatchId, batch.getId());
            if (saleEnterpriseId != null) {
                sWrapper.eq(TraceSale::getSaleEnterpriseId, saleEnterpriseId);
            }
            BigDecimal totalSold = traceSaleMapper.selectList(sWrapper).stream()
                    .filter(s -> s.getSaleQuantity() != null)
                    .map(TraceSale::getSaleQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalTransported.compareTo(BigDecimal.ZERO) > 0
                    && totalSold.compareTo(totalTransported) >= 0) {
                batch.setBatchStatus(8);
                batch.setUpdateTime(now);
                if (batch.getActualHarvestDate() == null) {
                    batch.setActualHarvestDate(java.time.LocalDate.now());
                }
                traceBatchMapper.updateById(batch);
                log.info("批次已完结: status=8, totalSold={}, totalTransported={}, batchId={}, batchCode={}",
                        totalSold, totalTransported, batch.getId(), batch.getBatchCode());
            } else {
                log.info("批次分批销售中: totalSold={}, totalTransported={}, batchId={}, batchCode={}",
                        totalSold, totalTransported, batch.getId(), batch.getBatchCode());
            }
        }

        // ====== 打包该批次全部加工链数据上链 ======
        if (batch != null) {
            try {
                String dataHash = buildProcessingChainHash(batch, record);
                String traceId = batch.getBatchCode() + "_SALE_" + record.getId();
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

                // 回写销售记录的上链信息
                record.setTxHash(txHash);
                record.setDataHash(dataHash);
                record.setBlockNumber(blockNumber);
                record.setChainTime(chainTime);
                record.setUpdateTime(LocalDateTime.now());
                traceSaleMapper.updateById(record);

                log.info("销售记录上链成功, batchCode={}, txHash={}, blockNumber={}",
                        batch.getBatchCode(), txHash, blockNumber);

                // 记录交易到 blockchain_transaction 表
                org.web3j.protocol.core.methods.response.TransactionReceipt fullReceipt =
                        web3jUtils.getTransactionReceipt(txHash);
                blockchainTransactionService.recordTransaction(txHash,
                        BlockchainTransactionService.BIZ_SALE, record.getId(),
                        batch.getId(), dataHash, fullReceipt);

                // 发送上链成功系统通知给企业全员
                try {
                    String enterpriseName = "";
                    EnterpriseInfo ent = enterpriseInfoMapper.selectById(batch.getEnterpriseId());
                    if (ent != null) {
                        enterpriseName = ent.getEnterpriseName();
                    }
                    String buyerName = record.getBuyerName() != null ? record.getBuyerName() : "未知";
                    String msgTitle = "【上链成功】销售记录已上链";
                    String msgSummary = "批次 " + batch.getBatchCode() + " 的销售记录已成功上链，买方：" + buyerName;
                    String msgContent = "上链成功通知\n\n"
                            + "企业名称：" + enterpriseName + "\n"
                            + "批次编号：" + batch.getBatchCode() + "\n"
                            + "产品名称：" + batch.getProductName() + "\n"
                            + "买方名称：" + buyerName + "\n"
                            + "交易哈希：" + txHash + "\n"
                            + "区块高度：" + blockNumber + "\n"
                            + "上链时间：" + chainTime;
                    messageService.sendToEnterprise(batch.getEnterpriseId(), "system", msgTitle, msgSummary, msgContent,
                            null, null, null);
                } catch (Exception msgEx) {
                    log.warn("发送上链通知失败(不影响业务): {}", msgEx.getMessage());
                }
            } catch (Exception e) {
                log.warn("销售记录上链失败(不影响业务): {}", e.getMessage());
            }
        }

        return record;
    }

    /**
     * 打包加工链全部数据生成哈希
     * 包含：批次信息 + 所有加工记录 + 所有运输记录 + 当前销售记录
     */
    private String buildProcessingChainHash(TraceBatch batch, TraceSale currentSale) {
        StringBuilder sb = new StringBuilder();

        // 1. 批次基本信息
        sb.append("BATCH|");
        sb.append("id=").append(batch.getId()).append("|");
        sb.append("code=").append(batch.getBatchCode()).append("|");
        sb.append("product=").append(batch.getProductName()).append("|");
        sb.append("type=").append(batch.getProductType()).append("|");
        sb.append("breed=").append(batch.getBreed()).append("|");
        sb.append("qty=").append(batch.getInitQuantity()).append("|");
        sb.append("enterprise=").append(batch.getEnterpriseId()).append("|");

        // 2. 所有加工记录
        List<TraceProcessing> processingList = traceProcessingMapper.selectList(
                new LambdaQueryWrapper<TraceProcessing>()
                        .eq(TraceProcessing::getBatchId, batch.getId())
                        .orderByAsc(TraceProcessing::getProcessingDate));
        sb.append("PROCESSING[").append(processingList.size()).append("]|");
        for (TraceProcessing p : processingList) {
            sb.append("P{method=").append(p.getProcessMethod())
              .append(",date=").append(p.getProcessingDate())
              .append(",specs=").append(p.getSpecs())
              .append(",in=").append(p.getInputQuantity())
              .append(",out=").append(p.getOutputQuantity())
              .append(",op=").append(p.getOperator())
              .append("}|");
        }

        // 3. 所有运输记录
        List<TraceTransport> transportList = traceTransportMapper.selectList(
                new LambdaQueryWrapper<TraceTransport>()
                        .eq(TraceTransport::getBatchId, batch.getId())
                        .orderByAsc(TraceTransport::getTransportDate));
        sb.append("TRANSPORT[").append(transportList.size()).append("]|");
        for (TraceTransport t : transportList) {
            sb.append("T{date=").append(t.getTransportDate())
              .append(",plate=").append(t.getPlateNumber())
              .append(",driver=").append(t.getDriverName())
              .append(",from=").append(t.getDepartureLocation())
              .append(",to=").append(t.getDestination())
              .append(",qty=").append(t.getTransportQuantity())
              .append("}|");
        }

        // 4. 当前销售记录
        sb.append("SALE{buyer=").append(currentSale.getBuyerName())
          .append(",date=").append(currentSale.getSaleDate())
          .append(",qty=").append(currentSale.getSaleQuantity())
          .append(",price=").append(currentSale.getSalePrice())
          .append(",channel=").append(currentSale.getSaleChannel())
          .append(",dest=").append(currentSale.getDestination())
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

    private TraceBatch findBatchByCode(String batchCode) {
        LambdaQueryWrapper<TraceBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TraceBatch::getBatchCode, batchCode.trim());
        TraceBatch batch = traceBatchMapper.selectOne(wrapper);
        if (batch == null) {
            throw new RuntimeException("batch not found for batchCode: " + batchCode);
        }
        return batch;
    }

    /**
     * 查询批次可销售数量
     * 仅支持加工企业：可销售 = SUM(transportQuantity WHERE batchId=? AND transportEnterpriseId=enterpriseId) - 已销售总量
     */
    public Map<String, Object> getAvailableSaleQuantity(Long batchId, Long enterpriseId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在");
        }

        // 该加工企业对该批次的运输总量
        LambdaQueryWrapper<TraceTransport> transportWrapper = new LambdaQueryWrapper<>();
        transportWrapper.eq(TraceTransport::getBatchId, batchId);
        if (enterpriseId != null) {
            transportWrapper.eq(TraceTransport::getTransportEnterpriseId, enterpriseId);
        }
        BigDecimal totalTransported = traceTransportMapper.selectList(transportWrapper).stream()
                .filter(t -> t.getTransportQuantity() != null)
                .map(TraceTransport::getTransportQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 该加工企业对该批次的已销售总量
        LambdaQueryWrapper<TraceSale> saleWrapper = new LambdaQueryWrapper<>();
        saleWrapper.eq(TraceSale::getBatchId, batchId);
        if (enterpriseId != null) {
            saleWrapper.eq(TraceSale::getSaleEnterpriseId, enterpriseId);
        }
        BigDecimal totalSold = traceSaleMapper.selectList(saleWrapper).stream()
                .filter(s -> s.getSaleQuantity() != null)
                .map(TraceSale::getSaleQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableSale = totalTransported.subtract(totalSold).max(BigDecimal.ZERO);

        Map<String, Object> result = new HashMap<>();
        result.put("totalTransported", totalTransported);
        result.put("totalSold", totalSold);
        result.put("availableSale", availableSale);
        result.put("calcBasis", "transport");
        result.put("unit", batch.getUnit() != null ? batch.getUnit() : "");
        result.put("productName", batch.getProductName() != null ? batch.getProductName() : "");
        return result;
    }

    /**
     * 更新销售记录
     */
    public void updateSale(TraceSale record) {
        if (record.getSaleEnterpriseId() == null && record.getEnterpriseId() != null) {
            record.setSaleEnterpriseId(record.getEnterpriseId());
        }
        record.setUpdateTime(java.time.LocalDateTime.now());
        traceSaleMapper.updateById(record);
        log.info("更新销售记录, id={}, batchId={}", record.getId(), record.getBatchId());
    }

    /**
     * 分页查询销售记录列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param batchId 批次ID（可选）
     * @param enterpriseId 企业ID（可选）
     * @param keyword 关键词（可选）
     * @return 分页结果
     */
    public Page<TraceSale> getSaleList(Long pageNum, Long pageSize, Long batchId, Long enterpriseId, String keyword) {
        Page<TraceSale> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<TraceSale> wrapper = new LambdaQueryWrapper<>();

        if (batchId != null) {
            wrapper.eq(TraceSale::getBatchId, batchId);
        }
        if (enterpriseId != null) {
            wrapper.eq(TraceSale::getSaleEnterpriseId, enterpriseId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w
                .like(TraceSale::getBatchCode, kw)
                .or().like(TraceSale::getBuyerName, kw)
                .or().like(TraceSale::getSaleChannel, kw)
            );
        }

        wrapper.orderByDesc(TraceSale::getCreateTime);

        return traceSaleMapper.selectPage(page, wrapper);
    }
}
