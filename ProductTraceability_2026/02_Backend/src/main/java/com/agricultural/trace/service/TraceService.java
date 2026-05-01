package com.agricultural.trace.service;

import com.agricultural.trace.entity.*;
import com.agricultural.trace.mapper.*;
import com.agricultural.trace.utils.Web3jUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 溯源详情服务
 * 从所有业务表聚合完整生命周期时间轴
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceService {

    private final TraceBatchMapper traceBatchMapper;
    private final TraceInspectionMapper traceInspectionMapper;
    private final TraceRecordMapper traceRecordMapper;
    private final TraceProcessingMapper traceProcessingMapper;
    private final TraceStorageMapper traceStorageMapper;
    private final TraceTransportMapper traceTransportMapper;
    private final TraceSaleMapper traceSaleMapper;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final Web3jUtils web3jUtils;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 获取完整溯源详情
     * 返回: { batchInfo, timeline[], enterpriseInfo }
     */
    public Map<String, Object> getTraceDetail(String batchId,
                                                String userIdentity,
                                                String ipAddress,
                                                String userAgent) {
        Map<String, Object> result = getTraceDetail(batchId);
        // record scan log for trace detail queries
        TraceBatch batch = findBatch(batchId);
        if (batch != null) {
            recordVerifyLog(batch, userIdentity, ipAddress, userAgent, null);
        }
        return result;
    }

    public Map<String, Object> getTraceDetail(String batchId) {
        TraceBatch batch = findBatch(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在: " + batchId);
        }

        EnterpriseInfo enterprise = null;
        if (batch.getEnterpriseId() != null) {
            enterprise = enterpriseInfoMapper.selectById(batch.getEnterpriseId());
        }

        Long bid = batch.getId();

        List<TraceRecord> records = traceRecordMapper.selectList(
                new LambdaQueryWrapper<TraceRecord>().eq(TraceRecord::getBatchId, bid).orderByAsc(TraceRecord::getRecordDate));
        List<TraceInspection> inspections = traceInspectionMapper.selectList(
                new LambdaQueryWrapper<TraceInspection>().eq(TraceInspection::getBatchId, bid).orderByAsc(TraceInspection::getInspectionDate));
        List<TraceProcessing> processings = traceProcessingMapper.selectList(
                new LambdaQueryWrapper<TraceProcessing>().eq(TraceProcessing::getBatchId, bid).orderByAsc(TraceProcessing::getProcessingDate));
        List<TraceStorage> storages = traceStorageMapper.selectList(
                new LambdaQueryWrapper<TraceStorage>().eq(TraceStorage::getBatchId, bid).orderByAsc(TraceStorage::getStorageDate));
        List<TraceTransport> transports = traceTransportMapper.selectList(
                new LambdaQueryWrapper<TraceTransport>().eq(TraceTransport::getBatchId, bid).orderByAsc(TraceTransport::getTransportDate));
        List<TraceSale> sales = traceSaleMapper.selectList(
                new LambdaQueryWrapper<TraceSale>().eq(TraceSale::getBatchId, bid).orderByAsc(TraceSale::getSaleDate));

        List<Map<String, Object>> timeline = buildTimeline(batch, records, inspections, processings, storages, transports, sales, enterprise);

        // batchInfo
        Map<String, Object> batchInfo = new LinkedHashMap<>();
        batchInfo.put("id", batch.getId());
        batchInfo.put("productName", batch.getProductName());
        batchInfo.put("batchCode", batch.getBatchCode());
        batchInfo.put("productionDate", batch.getProductionDate() != null ? batch.getProductionDate().format(DATE_FMT) : null);
        batchInfo.put("productType", batch.getProductType());
        batchInfo.put("productTypeName", getProductTypeName(batch.getProductType()));
        batchInfo.put("breed", batch.getBreed());
        batchInfo.put("initialQuantity", batch.getInitQuantity());
        batchInfo.put("unit", batch.getUnit());
        batchInfo.put("originLocation", normalizeDisplayLocation(batch.getOriginLocation()));
        batchInfo.put("latitude", batch.getLatitude());
        batchInfo.put("longitude", batch.getLongitude());
        batchInfo.put("manager", batch.getManager());
        batchInfo.put("status", batch.getBatchStatus());
        batchInfo.put("statusText", getStatusText(batch.getBatchStatus()));
        batchInfo.put("enterpriseName", enterprise != null ? enterprise.getEnterpriseName() : "");
        batchInfo.put("txHash", batch.getTxHash());
        batchInfo.put("blockNumber", batch.getBlockNumber());

        Map<String, Object> entInfo = null;
        if (enterprise != null) {
            entInfo = new LinkedHashMap<>();
            entInfo.put("id", enterprise.getId());
            entInfo.put("name", enterprise.getEnterpriseName());
            entInfo.put("logo", enterprise.getLogo() != null ? enterprise.getLogo() : "");
            entInfo.put("contactPhone", enterprise.getContactPhone() != null ? enterprise.getContactPhone() : "");
            entInfo.put("enterpriseCode", enterprise.getEnterpriseCode() != null ? enterprise.getEnterpriseCode() : "");
            entInfo.put("licenseNo", enterprise.getEnterpriseCode() != null ? enterprise.getEnterpriseCode() : "");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchInfo", batchInfo);
        result.put("timeline", timeline);
        result.put("processes", timeline);
        result.put("enterpriseInfo", entInfo);
        return result;
    }

    /**
     * 区块链验真
     *
     * 上链入口有两个，traceId 格式分别为：
     * - 养殖企业仓储上链: batchCode + "_STORAGE_" + storageRecordId
     * - 加工企业销售上链: batchCode + "_SALE_" + saleRecordId
     *
     * 验证时需要用与上链一致的 traceId 去链上查询
     */
    public Map<String, Object> verifyBlockchain(String batchId) {
        return verifyBlockchain(batchId, null, null, null, null);
    }

    public Map<String, Object> verifyBlockchain(String batchId,
                                                String userIdentity,
                                                String ipAddress,
                                                String userAgent,
                                                String scanLocation) {
        TraceBatch batch = findBatch(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在: " + batchId);
        }

        boolean allVerified = true;
        int totalNodes = 0, verifiedNodes = 0;
        List<Map<String, Object>> nodeResults = new ArrayList<>();

        // 1. 验证养殖批次初始化上链（traceId = batchCode_INIT）
        if (StringUtils.hasText(batch.getTxHash()) && StringUtils.hasText(batch.getDataHash())) {
            totalNodes++;
            boolean ok = false;
            String traceIdOnChain = batch.getBatchCode() + "_INIT";
            try { 
                ok = web3jUtils.verifyHash(traceIdOnChain, batch.getDataHash()); 
            } catch (Exception e) { 
                log.warn("批次哈希验证异常: traceId={}, err={}", traceIdOnChain, e.getMessage()); 
                ok = false;
            }
            if (ok) verifiedNodes++; else allVerified = false;
            Map<String, Object> nr = new HashMap<>();
            nr.put("stage", "batch_init"); nr.put("nodeName", "批次初始化");
            nr.put("verified", ok); nr.put("status", ok ? "valid" : "invalid");
            nr.put("txHash", batch.getTxHash());
            nodeResults.add(nr);
        }

        // 2. 验证生长记录（traceId = batchCode_RECORD_{recordId}）
        List<TraceRecord> records = traceRecordMapper.selectList(
                new LambdaQueryWrapper<TraceRecord>()
                        .eq(TraceRecord::getBatchId, batch.getId())
                        .isNotNull(TraceRecord::getTxHash));
        for (TraceRecord record : records) {
            if (StringUtils.hasText(record.getTxHash()) && StringUtils.hasText(record.getDataHash())) {
                totalNodes++;
                boolean ok = false;
                String traceIdOnChain = batch.getBatchCode() + "_RECORD_" + record.getId();
                try { 
                    ok = web3jUtils.verifyHash(traceIdOnChain, record.getDataHash()); 
                } catch (Exception e) { 
                    log.warn("生长记录哈希验证异常: traceId={}, err={}", traceIdOnChain, e.getMessage()); 
                    ok = false;
                }
                if (ok) verifiedNodes++; else allVerified = false;
                Map<String, Object> nr = new HashMap<>();
                nr.put("stage", "growth_record"); nr.put("nodeName", "生长记录");
                nr.put("verified", ok); nr.put("status", ok ? "valid" : "invalid");
                nr.put("txHash", record.getTxHash());
                nodeResults.add(nr);
            }
        }

        // 3. 验证检疫记录（traceId = batchCode_INSPECTION_{inspectionId}）
        List<TraceInspection> inspections = traceInspectionMapper.selectList(
                new LambdaQueryWrapper<TraceInspection>().eq(TraceInspection::getBatchId, batch.getId()));
        for (TraceInspection ins : inspections) {
            if (StringUtils.hasText(ins.getTxHash()) && StringUtils.hasText(ins.getDataHash())) {
                totalNodes++;
                boolean ok = false;
                String traceIdOnChain = batch.getBatchCode() + "_INSPECTION_" + ins.getId();
                try { 
                    ok = web3jUtils.verifyHash(traceIdOnChain, ins.getDataHash()); 
                } catch (Exception e) { 
                    log.warn("检疫哈希验证异常: traceId={}, err={}", traceIdOnChain, e.getMessage()); 
                    ok = false;
                }
                if (ok) verifiedNodes++; else allVerified = false;
                Map<String, Object> nr = new HashMap<>();
                nr.put("stage", "inspection"); nr.put("nodeName", "检疫质检");
                nr.put("verified", ok); nr.put("status", ok ? "valid" : "invalid");
                nr.put("txHash", ins.getTxHash()); nr.put("official", true);
                nodeResults.add(nr);
            }
        }

        // 4. 验证加工记录（traceId = sourceBatchCode_PROC_{processingId}）
        List<TraceProcessing> processings = traceProcessingMapper.selectList(
                new LambdaQueryWrapper<TraceProcessing>()
                        .eq(TraceProcessing::getBatchId, batch.getId())
                        .isNotNull(TraceProcessing::getTxHash));
        for (TraceProcessing proc : processings) {
            if (StringUtils.hasText(proc.getTxHash()) && StringUtils.hasText(proc.getDataHash())) {
                totalNodes++;
                boolean ok = false;
                String traceIdOnChain = proc.getSourceBatchCode() + "_PROC_" + proc.getId();
                try { 
                    ok = web3jUtils.verifyHash(traceIdOnChain, proc.getDataHash()); 
                } catch (Exception e) { 
                    log.warn("加工记录哈希验证异常: traceId={}, err={}", traceIdOnChain, e.getMessage()); 
                    ok = false;
                }
                if (ok) verifiedNodes++; else allVerified = false;
                Map<String, Object> nr = new HashMap<>();
                nr.put("stage", "processing"); nr.put("nodeName", "加工记录");
                nr.put("verified", ok); nr.put("status", ok ? "valid" : "invalid");
                nr.put("txHash", proc.getTxHash());
                nodeResults.add(nr);
            }
        }

        // 5. 验证仓储记录（traceId = batchCode_STORAGE_{storageId}）
        List<TraceStorage> storages = traceStorageMapper.selectList(
                new LambdaQueryWrapper<TraceStorage>()
                        .eq(TraceStorage::getBatchId, batch.getId())
                        .isNotNull(TraceStorage::getTxHash));
        for (TraceStorage storage : storages) {
            if (StringUtils.hasText(storage.getTxHash()) && StringUtils.hasText(storage.getDataHash())) {
                totalNodes++;
                boolean ok = false;
                String traceIdOnChain = batch.getBatchCode() + "_STORAGE_" + storage.getId();
                try { 
                    ok = web3jUtils.verifyHash(traceIdOnChain, storage.getDataHash()); 
                } catch (Exception e) { 
                    log.warn("仓储记录哈希验证异常: traceId={}, err={}", traceIdOnChain, e.getMessage()); 
                    ok = false;
                }
                if (ok) verifiedNodes++; else allVerified = false;
                Map<String, Object> nr = new HashMap<>();
                nr.put("stage", "storage"); nr.put("nodeName", "仓储入库");
                nr.put("verified", ok); nr.put("status", ok ? "valid" : "invalid");
                nr.put("txHash", storage.getTxHash());
                nodeResults.add(nr);
            }
        }

        // 6. 验证运输记录（traceId = batchCode_TRANSPORT_{transportId}）
        List<TraceTransport> transports = traceTransportMapper.selectList(
                new LambdaQueryWrapper<TraceTransport>()
                        .eq(TraceTransport::getBatchId, batch.getId())
                        .isNotNull(TraceTransport::getTxHash));
        for (TraceTransport transport : transports) {
            if (StringUtils.hasText(transport.getTxHash()) && StringUtils.hasText(transport.getDataHash())) {
                totalNodes++;
                boolean ok = false;
                String traceIdOnChain = batch.getBatchCode() + "_TRANSPORT_" + transport.getId();
                try { 
                    ok = web3jUtils.verifyHash(traceIdOnChain, transport.getDataHash()); 
                } catch (Exception e) { 
                    log.warn("运输记录哈希验证异常: traceId={}, err={}", traceIdOnChain, e.getMessage()); 
                    ok = false;
                }
                if (ok) verifiedNodes++; else allVerified = false;
                Map<String, Object> nr = new HashMap<>();
                nr.put("stage", "transport"); nr.put("nodeName", "物流运输");
                nr.put("verified", ok); nr.put("status", ok ? "valid" : "invalid");
                nr.put("txHash", transport.getTxHash());
                nodeResults.add(nr);
            }
        }

        // 7. 验证销售记录（traceId = batchCode_SALE_{saleId}）
        List<TraceSale> sales = traceSaleMapper.selectList(
                new LambdaQueryWrapper<TraceSale>()
                        .eq(TraceSale::getBatchId, batch.getId())
                        .isNotNull(TraceSale::getTxHash));
        for (TraceSale sale : sales) {
            if (StringUtils.hasText(sale.getTxHash()) && StringUtils.hasText(sale.getDataHash())) {
                totalNodes++;
                boolean ok = false;
                String traceIdOnChain = batch.getBatchCode() + "_SALE_" + sale.getId();
                try { 
                    ok = web3jUtils.verifyHash(traceIdOnChain, sale.getDataHash()); 
                } catch (Exception e) { 
                    log.warn("销售哈希验证异常: traceId={}, err={}", traceIdOnChain, e.getMessage()); 
                    ok = false;
                }
                if (ok) verifiedNodes++; else allVerified = false;
                Map<String, Object> nr = new HashMap<>();
                nr.put("stage", "sale"); nr.put("nodeName", "销售出库");
                nr.put("verified", ok); nr.put("status", ok ? "valid" : "invalid");
                nr.put("txHash", sale.getTxHash());
                nodeResults.add(nr);
            }
        }

        if (totalNodes == 0) allVerified = true;

        // 获取企业信息
        String enterpriseName = null;
        if (batch.getEnterpriseId() != null) {
            EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(batch.getEnterpriseId());
            if (enterprise != null) {
                enterpriseName = enterprise.getEnterpriseName();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("verified", allVerified);
        result.put("valid", allVerified);
        result.put("message", allVerified ? "全部数据验证通过" : "数据完整性存在异常");
        result.put("batchCode", batch.getBatchCode());
        result.put("productName", batch.getProductName());
        result.put("enterpriseName", enterpriseName);
        result.put("localHash", batch.getDataHash());
        result.put("chainHash", batch.getDataHash());
        result.put("txHash", batch.getTxHash());
        result.put("totalNodes", totalNodes);
        result.put("verifiedNodes", verifiedNodes);
        result.put("blockNumber", batch.getBlockNumber());
        result.put("chainTime", batch.getChainTime());
        result.put("details", nodeResults);
        result.put("nodeResults", nodeResults);

        recordVerifyLog(batch, userIdentity, ipAddress, userAgent, scanLocation);
        return result;
    }

    // ==================== 内部方法 ====================

    private TraceBatch findBatch(String batchId) {
        if (batchId == null || batchId.isEmpty()) return null;
        try {
            Long id = Long.parseLong(batchId);
            TraceBatch batch = traceBatchMapper.selectById(id);
            if (batch != null) return batch;
        } catch (NumberFormatException ignored) {}
        return traceBatchMapper.selectOne(
                new LambdaQueryWrapper<TraceBatch>().eq(TraceBatch::getBatchCode, batchId.trim()));
    }

    /**
     * 构建全生命周期时间轴(从真实数据表)
     */
    private List<Map<String, Object>> buildTimeline(
            TraceBatch batch, List<TraceRecord> records, List<TraceInspection> inspections,
            List<TraceProcessing> processings, List<TraceStorage> storages,
            List<TraceTransport> transports, List<TraceSale> sales, EnterpriseInfo enterprise) {

        List<Map<String, Object>> timeline = new ArrayList<>();
        String enterpriseName = enterprise != null ? enterprise.getEnterpriseName() : "";

        // -- 养殖初始化 --
        Map<String, Object> initNode = new LinkedHashMap<>();
        initNode.put("stage", "batch_init");
        initNode.put("title", "养殖初始化");
        initNode.put("time", formatDateTime(batch.getCreateTime()));
        initNode.put("operator", batch.getManager() != null ? batch.getManager() : enterpriseName);
        initNode.put("txHash", batch.getTxHash());
        initNode.put("blockNumber", batch.getBlockNumber());
        Map<String, Object> initD = new LinkedHashMap<>();
        initD.put("产品名称", batch.getProductName());
        initD.put("品种", batch.getBreed());
        String qtyStr = (batch.getInitQuantity() != null ? batch.getInitQuantity().stripTrailingZeros().toPlainString() : "--")
                + (batch.getUnit() != null ? batch.getUnit() : "");
        initD.put("数量", qtyStr);
        initD.put("产地", normalizeDisplayLocation(batch.getOriginLocation()));
        initD.put("来源企业", enterpriseName);
        initNode.put("details", initD);
        initNode.put("images", new ArrayList<>());
        initNode.put("latitude", batch.getLatitude());
        initNode.put("longitude", batch.getLongitude());
        timeline.add(initNode);

        // -- 生长记录 (trace_record) --
        for (TraceRecord r : records) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("stage", "growth");
            node.put("title", getRecordTypeText(r.getRecordType()));
            node.put("time", r.getRecordDate() != null ? r.getRecordDate().format(DATE_FMT) : formatDateTime(r.getCreateTime()));
            node.put("operator", r.getOperator());
            node.put("txHash", r.getTxHash());
            node.put("blockNumber", r.getBlockNumber());
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("记录类型", getRecordTypeText(r.getRecordType()));
            d.put("物料名称", r.getItemName());
            d.put("用量", r.getAmount() != null ? r.getAmount().stripTrailingZeros().toPlainString() : null);
            d.put("操作说明", r.getDescription());
            d.put("作业地点", normalizeDisplayLocation(r.getLocation()));
            node.put("details", d);
            node.put("images", parseImages(r.getImages()));
            timeline.add(node);
        }

        // -- 检疫质检 (trace_inspection) --
        for (TraceInspection ins : inspections) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("stage", "inspection");
            node.put("title", "检疫质检");
            node.put("time", ins.getInspectionDate() != null ? ins.getInspectionDate().format(DATE_FMT) : formatDateTime(ins.getCreateTime()));
            node.put("operator", ins.getInspector());
            node.put("txHash", ins.getTxHash());
            node.put("blockNumber", ins.getBlockNumber());
            node.put("official", true);
            Map<String, Object> d = new LinkedHashMap<>();
            String resultText = ins.getCheckResult() != null && ins.getCheckResult() == 1 ? "合格" : "不合格";
            d.put("检疫结果", resultText);
            d.put("证书编号", ins.getCertNo());
            d.put("检测项目", ins.getInspectionItems());
            d.put("检疫员", ins.getInspector());
            d.put("检疫员证号", ins.getInspectorCode());
            node.put("details", d);
            node.put("images", parseImages(ins.getCertImage()));
            timeline.add(node);
        }

        // -- 加工处理 (trace_processing) --
        for (TraceProcessing p : processings) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("stage", "processing");
            node.put("title", "加工处理");
            node.put("time", p.getProcessingDate() != null ? p.getProcessingDate().format(DATE_FMT) : formatDateTime(p.getCreateTime()));
            node.put("operator", p.getOperator());
            node.put("txHash", p.getTxHash());
            node.put("blockNumber", p.getBlockNumber());
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("加工方式", p.getProcessMethod());
            d.put("包装规格", p.getSpecs());
            String inQty = p.getInputQuantity() != null ? p.getInputQuantity().stripTrailingZeros().toPlainString() : null;
            String inUnit = p.getInputUnit() != null ? p.getInputUnit() : "";
            d.put("投入数量", inQty != null ? inQty + inUnit : null);
            String outQty = p.getOutputQuantity() != null ? p.getOutputQuantity().stripTrailingZeros().toPlainString() : null;
            String outUnit = p.getOutputUnit() != null ? p.getOutputUnit() : "";
            d.put("产出数量", outQty != null ? outQty + outUnit : null);
            if (p.getProcessingEnterpriseId() != null) {
                EnterpriseInfo pe = enterpriseInfoMapper.selectById(p.getProcessingEnterpriseId());
                d.put("加工企业", pe != null ? pe.getEnterpriseName() : "");
            }
            node.put("details", d);
            node.put("images", parseImages(p.getImages()));
            timeline.add(node);
        }

        // -- 仓储入库 (trace_storage) --
        for (TraceStorage s : storages) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("stage", "storage");
            node.put("title", s.getStorageType() == 1 ? "仓储入库" : s.getStorageType() == 2 ? "仓储出库" : "库存盘点");
            node.put("time", s.getStorageDate() != null ? s.getStorageDate().format(DATE_FMT) : formatDateTime(s.getCreateTime()));
            node.put("operator", s.getOperator());
            node.put("txHash", s.getTxHash());
            node.put("blockNumber", s.getBlockNumber());
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("仓库名称", s.getWarehouseName());
            d.put("仓库位置", normalizeDisplayLocation(s.getWarehouseLocation()));
            d.put("数量", s.getStorageQuantity() != null ? s.getStorageQuantity().stripTrailingZeros().toPlainString() : null);
            d.put("温度", s.getTemperature() != null ? s.getTemperature() + "C" : null);
            d.put("湿度", s.getHumidity() != null ? s.getHumidity() + "%" : null);
            d.put("存储条件", s.getStorageCondition());
            node.put("details", d);
            node.put("images", parseImages(s.getImages()));
            timeline.add(node);
        }

        // -- 物流运输 (trace_transport) --
        for (TraceTransport t : transports) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("stage", "transport");
            node.put("title", "物流运输");
            node.put("time", t.getTransportDate() != null ? t.getTransportDate().format(DATE_FMT) : formatDateTime(t.getCreateTime()));
            node.put("operator", t.getDriverName());
            node.put("txHash", t.getTxHash());
            node.put("blockNumber", t.getBlockNumber());
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("物流单号", t.getLogisticsNo());
            d.put("车牌号", t.getPlateNumber());
            d.put("司机", t.getDriverName());
            d.put("司机电话", t.getDriverPhone());
            d.put("收货人", t.getReceiverName());
            d.put("出发地", normalizeDisplayLocation(t.getDepartureLocation()));
            d.put("目的地", t.getDestination());
            d.put("温度", t.getTemperature() != null ? t.getTemperature() + "C" : null);
            d.put("数量", t.getTransportQuantity() != null ? t.getTransportQuantity().stripTrailingZeros().toPlainString() : null);
            node.put("details", d);
            node.put("images", parseImages(t.getImages()));
            timeline.add(node);
        }

        // -- 销售记录 (trace_sale) --
        for (TraceSale sl : sales) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("stage", "sale");
            node.put("title", "销售出库");
            node.put("time", sl.getSaleDate() != null ? sl.getSaleDate().format(DATE_FMT) : formatDateTime(sl.getCreateTime()));
            node.put("operator", sl.getBuyerName());
            node.put("txHash", sl.getTxHash());
            node.put("blockNumber", sl.getBlockNumber());
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("购买方", sl.getBuyerName());
            d.put("数量", sl.getSaleQuantity() != null ? sl.getSaleQuantity().stripTrailingZeros().toPlainString() : null);
            d.put("单价", sl.getSalePrice() != null ? sl.getSalePrice() + "元" : null);
            d.put("总额", sl.getTotalAmount() != null ? sl.getTotalAmount() + "元" : null);
            d.put("销售渠道", sl.getSaleChannel());
            d.put("目的地", sl.getDestination());
            node.put("details", d);
            node.put("images", parseImages(sl.getSaleVoucher()));
            timeline.add(node);
        }

        // 按时间正序
        timeline.sort((a, b) -> {
            String ta = (String) a.getOrDefault("time", "");
            String tb = (String) b.getOrDefault("time", "");
            return ta.compareTo(tb);
        });

        return timeline;
    }

    private List<String> parseImages(String imgStr) {
        List<String> images = new ArrayList<>();
        if (!StringUtils.hasText(imgStr)) return images;
        imgStr = imgStr.trim();
        if (imgStr.startsWith("[")) {
            imgStr = imgStr.replaceAll("[\\[\\]\"]", "");
        }
        for (String s : imgStr.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) images.add(trimmed);
        }
        return images;
    }

    private String getRecordTypeText(String type) {
        if (type == null) return "操作记录";
        switch (type) {
            case "feeding": return "喂养记录";
            case "vaccine": return "防疫记录";
            case "inspect": return "巡查记录";
            case "fertilize": return "施肥记录";
            case "irrigate": return "浇水记录";
            case "pesticide": return "用药记录";
            default: return type;
        }
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DATETIME_FMT);
    }

    private String getProductTypeName(Integer type) {
        if (type == null) return "农产品";
        switch (type) {
            case 1: return "种植类";
            case 2: return "养殖类";
            default: return "农产品";
        }
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
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
    private String normalizeDisplayLocation(String rawLocation) {
        if (!StringUtils.hasText(rawLocation)) {
            return rawLocation;
        }
        String trimmed = rawLocation.trim();
        if (trimmed.matches("^-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?$")) {
            return "已记录定位坐标";
        }
        return trimmed;
    }

    private void recordVerifyLog(TraceBatch batch,
                                 String userIdentity,
                                 String ipAddress,
                                 String userAgent,
                                 String scanLocation) {
        if (batch == null || !StringUtils.hasText(batch.getBatchCode())) {
            return;
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO user_scan_log " +
                            "(batch_code, batch_id, openid, scan_location, ip_address, device_type, user_agent) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{
                            batch.getBatchCode(),
                            batch.getId(),
                            emptyToNull(userIdentity),
                            emptyToNull(scanLocation),
                            emptyToNull(ipAddress),
                            resolveDeviceType(userAgent),
                            emptyToNull(userAgent)
                    }
            );
        } catch (Exception e) {
            log.warn("记录验真日志失败, batchCode={}, err={}", batch.getBatchCode(), e.getMessage());
        }
    }

    private String resolveDeviceType(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "unknown";
        }
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("iphone") || normalized.contains("android")
                || normalized.contains("mobile") || normalized.contains("micromessenger")) {
            return "mobile";
        }
        if (normalized.contains("ipad") || normalized.contains("tablet")) {
            return "tablet";
        }
        if (normalized.contains("windows") || normalized.contains("macintosh") || normalized.contains("linux")) {
            return "pc";
        }
        return "unknown";
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
