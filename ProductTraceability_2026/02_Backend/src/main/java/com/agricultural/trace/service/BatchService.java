package com.agricultural.trace.service;

import com.agricultural.trace.dto.BatchQueryDTO;
import com.agricultural.trace.dto.BatchReceiveDTO;
import com.agricultural.trace.entity.EnterpriseInfo;
import com.agricultural.trace.entity.TraceBatch;
import com.agricultural.trace.entity.TraceInspection;
import com.agricultural.trace.entity.TraceRecord;
import com.agricultural.trace.entity.TraceProcessing;
import com.agricultural.trace.entity.TraceStorage;
import com.agricultural.trace.entity.TraceTransport;
import com.agricultural.trace.mapper.EnterpriseInfoMapper;
import com.agricultural.trace.mapper.TraceBatchMapper;
import com.agricultural.trace.mapper.TraceInspectionMapper;
import com.agricultural.trace.mapper.TraceProcessingMapper;
import com.agricultural.trace.mapper.TraceRecordMapper;
import com.agricultural.trace.mapper.TraceStorageMapper;
import com.agricultural.trace.mapper.TraceTransportMapper;
import com.agricultural.trace.utils.Web3jUtils;
import com.agricultural.trace.vo.BatchVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 批次服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private final TraceBatchMapper traceBatchMapper;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final TraceInspectionMapper traceInspectionMapper;
    private final TraceRecordMapper traceRecordMapper;
    private final TraceProcessingMapper traceProcessingMapper;
    private final TraceStorageMapper traceStorageMapper;
    private final Web3jUtils web3jUtils;
    private final BlockchainTransactionService blockchainTransactionService;
    private final StorageService storageService;
    private final MessageService messageService;
    private final TraceTransportMapper traceTransportMapper;

    @Value("${server.port:8888}")
    private int serverPort;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 创建新批次
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createBatch(Map<String, Object> params) {
        TraceBatch batch = new TraceBatch();

        // 优先使用前端传入的批次号，没有则自动生成
        String batchCode = toNullableString(params.get("batchCode"));
        if (batchCode == null) {
            batchCode = "BATCH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + String.format("%04d", (int)(Math.random() * 10000));
        }
        batch.setBatchCode(batchCode);

        // 基本信息
        if (params.get("productType") != null) {
            batch.setProductType(Integer.parseInt(params.get("productType").toString()));
        }
        // productName: if not provided, auto-fill based on productType
        String productName = params.get("productName") != null ? params.get("productName").toString().trim() : null;
        if (productName == null || productName.isEmpty()) {
            productName = batch.getProductType() != null && batch.getProductType() == 2 ? "西红柿" : "肉鸡";
        }
        batch.setProductName(productName);
        batch.setBreed(params.get("breed") != null ? params.get("breed").toString() : null);

        // unit: if not provided, auto-fill based on productType
        String unit = params.get("unit") != null ? params.get("unit").toString().trim() : null;
        if (unit == null || unit.isEmpty()) {
            unit = batch.getProductType() != null && batch.getProductType() == 2 ? "株" : "只";
        }

        Object quantityVal = params.get("initQuantity") != null ? params.get("initQuantity") : params.get("quantity");
        if (quantityVal != null) {
            String qStr = quantityVal.toString().trim();
            if (!qStr.isEmpty()) {
                batch.setInitQuantity(new java.math.BigDecimal(qStr));
                batch.setCurrentQuantity(new java.math.BigDecimal(qStr));
            }
        }
        batch.setUnit(unit);

        // 产地信息
        batch.setOriginLocation(params.get("originLocation") != null ? params.get("originLocation").toString() : null);
        if (params.get("latitude") != null) {
            batch.setLatitude(new java.math.BigDecimal(params.get("latitude").toString()));
        }
        if (params.get("longitude") != null) {
            batch.setLongitude(new java.math.BigDecimal(params.get("longitude").toString()));
        }

        // 特定字段
        batch.setManager(toNullableString(params.get("manager")));
        batch.setPlantArea(toNullableDecimal(params.get("plantArea")));
        batch.setGreenhouseNo(toNullableString(params.get("greenhouseNo")));
        batch.setSeedSource(toNullableString(params.get("seedSource")));

        // 企业ID
        if (params.get("enterpriseId") != null) {
            batch.setEnterpriseId(Long.parseLong(params.get("enterpriseId").toString()));
        }

        // 日期字段
        String prodDateStr = toNullableString(params.get("productionDate"));
        if (prodDateStr != null) {
            batch.setProductionDate(java.time.LocalDate.parse(prodDateStr));
        } else {
            batch.setProductionDate(java.time.LocalDate.now());
        }
        String expectedDateStr = toNullableString(params.get("expectedHarvestDate"));
        if (expectedDateStr != null) {
            batch.setExpectedHarvestDate(java.time.LocalDate.parse(expectedDateStr));
        }

        // 初始状态
        batch.setBatchStatus(1);
        batch.setCreateTime(LocalDateTime.now());
        batch.setUpdateTime(LocalDateTime.now());

        traceBatchMapper.insert(batch);

        // Auto-generate qr_code_url after batch is created (id is available)
        String qrCodeUrl = "http://api.domain.com/trace?code=" + batchCode;
        batch.setQrCodeUrl(qrCodeUrl);
        traceBatchMapper.updateById(batch);
        notifyAdmins(
                "批次初始化已提交",
                buildEnterprisePrefix(batch.getEnterpriseId()) + "创建了批次 " + batchCode,
                "企业名称：" + resolveEnterpriseName(batch.getEnterpriseId())
                        + "\n批次编号：" + batchCode
                        + "\n产品名称：" + batch.getProductName()
                        + "\n初始数量：" + (batch.getInitQuantity() != null ? batch.getInitQuantity() : "")
                        + " " + (batch.getUnit() != null ? batch.getUnit() : "")
                        + "\n请及时关注后续流转。",
                "/supervision/trace"
        );

        log.info("创建批次成功, batchCode={}, productName={}, qrCodeUrl={}", batchCode, batch.getProductName(), qrCodeUrl);

        // ====== 批次初始化独立上链 ======
        try {
            String dataHash = computeSHA256("BATCH_INIT|code=" + batchCode
                    + "|product=" + batch.getProductName()
                    + "|type=" + batch.getProductType()
                    + "|qty=" + batch.getInitQuantity()
                    + "|enterprise=" + batch.getEnterpriseId()
                    + "|ts=" + System.currentTimeMillis());
            String traceId = batchCode + "_INIT";
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
            batch.setTxHash(txHash);
            batch.setDataHash(dataHash);
            batch.setBlockNumber(blockNumber);
            batch.setChainTime(chainTime);
            batch.setUpdateTime(LocalDateTime.now());
            traceBatchMapper.updateById(batch);

            log.info("批次初始化上链成功, batchCode={}, txHash={}, blockNumber={}", batchCode, txHash, blockNumber);

            // 记录交易到 blockchain_transaction 表
            org.web3j.protocol.core.methods.response.TransactionReceipt fullReceipt =
                    web3jUtils.getTransactionReceipt(txHash);
            blockchainTransactionService.recordTransaction(txHash,
                    BlockchainTransactionService.BIZ_BATCH, batch.getId(),
                    batch.getId(), dataHash, fullReceipt);
        } catch (Exception e) {
            log.warn("批次初始化上链失败(不影响业务): {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", batch.getId());
        result.put("batchCode", batchCode);
        result.put("qrCodeUrl", qrCodeUrl);
        return result;
    }

    /**
     * 分页查询批次列表
     * 支持多状态查询（statuses 数组）
     */
    public Page<BatchVO> getBatchList(BatchQueryDTO queryDTO) {
        Page<TraceBatch> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        LambdaQueryWrapper<TraceBatch> wrapper = new LambdaQueryWrapper<>();

        // 检疫机构筛选：只显示分配给该检疫机构的批次（通过 trace_inspection 关联）
        if (queryDTO.getInspectionEnterpriseId() != null) {
            log.info("检疫企业查询批次，inspectionEnterpriseId={}", queryDTO.getInspectionEnterpriseId());
            wrapper.inSql(TraceBatch::getId,
                "SELECT DISTINCT batch_id FROM trace_inspection WHERE inspection_enterprise_id = "
                    + queryDTO.getInspectionEnterpriseId());
        }

        // 企业ID筛选 - 根据企业类型使用不同的查询逻辑
        if (queryDTO.getEnterpriseId() != null) {
            Integer enterpriseType = queryDTO.getEnterpriseType();
            
            if (enterpriseType != null && enterpriseType == 2) {
                // Processing enterprise query logic:
                // 1. Status 7 (in transit) batches where receiveEnterpriseId = this enterprise (pending receive)
                // 2. Status 4 batches where receiveEnterpriseId = this enterprise (just received)
                // 3. Batches with storage/processing records belonging to this enterprise
                log.info("加工企业查询批次，enterpriseId={}", queryDTO.getEnterpriseId());
                
                Long eid = queryDTO.getEnterpriseId();
                wrapper.and(w -> w
                    // Pending receive: in transit, target is this enterprise
                    .eq(TraceBatch::getReceiveEnterpriseId, eid)
                    .or(subW -> subW
                        .inSql(TraceBatch::getId,
                            "SELECT DISTINCT batch_id FROM trace_transport WHERE receive_enterprise_id = " + eid)
                    )
                    .or(subW -> subW
                        .inSql(TraceBatch::getId,
                            "SELECT DISTINCT batch_id FROM trace_storage WHERE storage_enterprise_id = " + eid)
                    )
                    .or(subW -> subW
                        .inSql(TraceBatch::getId,
                            "SELECT DISTINCT batch_id FROM trace_processing WHERE processing_enterprise_id = " + eid)
                    )
                );
                if (Boolean.TRUE.equals(queryDTO.getExcludeProcessedCreated())) {
                    wrapper.notInSql(
                            TraceBatch::getId,
                            "SELECT DISTINCT batch_id FROM trace_processing"
                    );
                }
                if (Boolean.TRUE.equals(queryDTO.getExcludeStorageEntered())) {
                    wrapper.notInSql(
                            TraceBatch::getId,
                            "SELECT DISTINCT batch_id FROM trace_storage WHERE storage_enterprise_id = "
                                    + eid + " AND storage_type = 1"
                    );
                }
            } else {
                // 养殖企业或其他：按批次创建企业ID筛选
                log.info("养殖企业查询批次，enterpriseId={}", queryDTO.getEnterpriseId());
                wrapper.eq(TraceBatch::getEnterpriseId, queryDTO.getEnterpriseId());
                if (Boolean.TRUE.equals(queryDTO.getExcludeStorageEntered())) {
                    Long eid = queryDTO.getEnterpriseId();
                    wrapper.notInSql(
                            TraceBatch::getId,
                            "SELECT DISTINCT batch_id FROM trace_storage WHERE storage_enterprise_id = "
                                    + eid + " AND storage_type = 1"
                    );
                }
            }
        }

        // 多状态筛选（核心功能）
        if (queryDTO.getStatuses() != null && !queryDTO.getStatuses().isEmpty()) {
            wrapper.in(TraceBatch::getBatchStatus, queryDTO.getStatuses());
        }

        // 产品类型筛选
        if (queryDTO.getProductType() != null) {
            wrapper.eq(TraceBatch::getProductType, queryDTO.getProductType());
        }

        // 关键词搜索（批次号或产品名称）
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = queryDTO.getKeyword().trim();
            wrapper.and(w -> w
                .like(TraceBatch::getBatchCode, keyword)
                .or()
                .like(TraceBatch::getProductName, keyword)
            );
        }

        // 批次号精确搜索
        if (StringUtils.hasText(queryDTO.getBatchCode())) {
            wrapper.like(TraceBatch::getBatchCode, queryDTO.getBatchCode().trim());
        }

        // 产品名称搜索
        if (StringUtils.hasText(queryDTO.getProductName())) {
            wrapper.like(TraceBatch::getProductName, queryDTO.getProductName().trim());
        }

        // 是否已上链筛选
        if (queryDTO.getHasChain() != null) {
            if (queryDTO.getHasChain()) {
                wrapper.isNotNull(TraceBatch::getTxHash);
                wrapper.ne(TraceBatch::getTxHash, "");
            } else {
                wrapper.and(w -> w.isNull(TraceBatch::getTxHash).or().eq(TraceBatch::getTxHash, ""));
            }
        }

        // 按创建时间倒序
        wrapper.orderByDesc(TraceBatch::getCreateTime);

        // 查询
        Page<TraceBatch> batchPage = traceBatchMapper.selectPage(page, wrapper);

        // 转换为 VO
        Page<BatchVO> voPage = new Page<>(batchPage.getCurrent(), batchPage.getSize(), batchPage.getTotal());
        
        if (batchPage.getRecords() != null && !batchPage.getRecords().isEmpty()) {
            // 批量获取企业信息
            List<Long> enterpriseIds = batchPage.getRecords().stream()
                .map(TraceBatch::getEnterpriseId)
                .distinct()
                .collect(Collectors.toList());
            
            Map<Long, EnterpriseInfo> enterpriseMap = new HashMap<>();
            if (!enterpriseIds.isEmpty()) {
                List<EnterpriseInfo> enterprises = enterpriseInfoMapper.selectBatchIds(enterpriseIds);
                enterpriseMap = enterprises.stream()
                    .collect(Collectors.toMap(EnterpriseInfo::getId, e -> e));
            }

            // 转换记录
            Map<Long, EnterpriseInfo> finalEnterpriseMap = enterpriseMap;
            List<BatchVO> voList = batchPage.getRecords().stream()
                .map(batch -> convertToVO(batch, finalEnterpriseMap.get(batch.getEnterpriseId())))
                .collect(Collectors.toList());
            
            voPage.setRecords(voList);
        }

        return voPage;
    }

    /**
     * 统计各状态的批次数量
     */
    public Map<String, Long> getStatusCount(Long enterpriseId, Integer enterpriseType) {
        Map<String, Long> countMap = new HashMap<>();

        // Processing enterprise: count by receiveEnterpriseId and transport records
        if (enterpriseId != null && enterpriseType != null && enterpriseType == 2) {
            // pendingReceive: batches with status 5 or 7 that target this enterprise
            LambdaQueryWrapper<TraceBatch> pendingWrapper = new LambdaQueryWrapper<>();
            pendingWrapper.in(TraceBatch::getBatchStatus, 5, 7);
            pendingWrapper.and(w -> w
                .eq(TraceBatch::getReceiveEnterpriseId, enterpriseId)
                .or(subW -> subW
                    .inSql(TraceBatch::getId,
                        "SELECT DISTINCT batch_id FROM trace_transport WHERE receive_enterprise_id = " + enterpriseId)
                )
            );
            countMap.put("pendingReceive", traceBatchMapper.selectCount(pendingWrapper));

            // processing: status 4, related to this enterprise
            LambdaQueryWrapper<TraceBatch> processingWrapper = new LambdaQueryWrapper<>();
            processingWrapper.eq(TraceBatch::getBatchStatus, 4);
            processingWrapper.and(w -> w
                .eq(TraceBatch::getReceiveEnterpriseId, enterpriseId)
                .or(subW -> subW
                    .inSql(TraceBatch::getId,
                        "SELECT DISTINCT batch_id FROM trace_processing WHERE processing_enterprise_id = " + enterpriseId)
                )
            );
            countMap.put("processing", traceBatchMapper.selectCount(processingWrapper));

            // completed: status 6,8,9 related to this enterprise
            LambdaQueryWrapper<TraceBatch> completedWrapper = new LambdaQueryWrapper<>();
            completedWrapper.in(TraceBatch::getBatchStatus, 6, 8, 9);
            completedWrapper.and(w -> w
                .eq(TraceBatch::getReceiveEnterpriseId, enterpriseId)
                .or(subW -> subW
                    .inSql(TraceBatch::getId,
                        "SELECT DISTINCT batch_id FROM trace_storage WHERE storage_enterprise_id = " + enterpriseId)
                )
                .or(subW -> subW
                    .inSql(TraceBatch::getId,
                        "SELECT DISTINCT batch_id FROM trace_processing WHERE processing_enterprise_id = " + enterpriseId)
                )
            );
            countMap.put("completed", completedWrapper != null ? traceBatchMapper.selectCount(completedWrapper) : 0L);

            return countMap;
        }

        // Default logic for farming enterprises and others
        LambdaQueryWrapper<TraceBatch> baseWrapper = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            baseWrapper.eq(TraceBatch::getEnterpriseId, enterpriseId);
        }

        // breeding count (status 1, 2)
        LambdaQueryWrapper<TraceBatch> breedingWrapper = baseWrapper.clone();
        breedingWrapper.in(TraceBatch::getBatchStatus, 1, 2);
        countMap.put("breeding", traceBatchMapper.selectCount(breedingWrapper));

        // pending count (status 3)
        LambdaQueryWrapper<TraceBatch> pendingWrapper = baseWrapper.clone();
        pendingWrapper.eq(TraceBatch::getBatchStatus, 3);
        countMap.put("pending", traceBatchMapper.selectCount(pendingWrapper));

        // completed count (status 4, 5, 6, 7, 8, 9)
        LambdaQueryWrapper<TraceBatch> completedWrapper = baseWrapper.clone();
        completedWrapper.in(TraceBatch::getBatchStatus, 4, 5, 6, 7, 8, 9);
        countMap.put("completed", traceBatchMapper.selectCount(completedWrapper));

        return countMap;
    }

    /**
     * 获取批次详细信息（供详情页、检疫录入、报告上传等页面使用）
     * 支持 batchCode 和 id 两种查询方式，batchCode 优先
     * 所有角色均可调用
     *
     * @param batchCode 批次编号（优先）
     * @param id 批次ID
     * @return 批次完整详情信息
     */
    public Map<String, Object> getBatchDetail(String batchCode, Long id) {
        TraceBatch batch = null;

        // 优先按 batchCode 查询
        if (StringUtils.hasText(batchCode)) {
            LambdaQueryWrapper<TraceBatch> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TraceBatch::getBatchCode, batchCode.trim());
            batch = traceBatchMapper.selectOne(wrapper);
        }

        // 其次按 id 查询
        if (batch == null && id != null) {
            batch = traceBatchMapper.selectById(id);
        }

        if (batch == null) {
            throw new RuntimeException("批次不存在" + (StringUtils.hasText(batchCode) ? "，batchCode: " + batchCode : "，id: " + id));
        }

        // 查询企业信息
        EnterpriseInfo enterprise = null;
        if (batch.getEnterpriseId() != null) {
            enterprise = enterpriseInfoMapper.selectById(batch.getEnterpriseId());
        }

        // 查询检疫记录（最新一条）
        LambdaQueryWrapper<TraceInspection> inspectionWrapper = new LambdaQueryWrapper<>();
        inspectionWrapper.eq(TraceInspection::getBatchId, batch.getId());
        inspectionWrapper.orderByDesc(TraceInspection::getInspectionDate);
        inspectionWrapper.last("LIMIT 1");
        TraceInspection inspection = traceInspectionMapper.selectOne(inspectionWrapper);

        // 组装返回数据 (严格对齐 trace_batch 全字段)
        Map<String, Object> result = new HashMap<>();
        result.put("id", batch.getId());
        result.put("batchId", batch.getId());
        result.put("batchCode", batch.getBatchCode());
        result.put("productName", batch.getProductName());
        result.put("productType", batch.getProductType());
        result.put("breed", batch.getBreed());
        result.put("initQuantity", batch.getInitQuantity());
        result.put("currentQuantity", batch.getCurrentQuantity());
        result.put("quantity", batch.getCurrentQuantity() != null ? batch.getCurrentQuantity() : batch.getInitQuantity());
        result.put("unit", batch.getUnit());
        result.put("originLocation", normalizeDisplayLocation(batch.getOriginLocation()));
        result.put("latitude", batch.getLatitude());
        result.put("longitude", batch.getLongitude());
        result.put("plantArea", batch.getPlantArea());
        result.put("greenhouseNo", batch.getGreenhouseNo());
        result.put("seedSource", batch.getSeedSource());
        result.put("manager", batch.getManager());
        result.put("batchStatus", batch.getBatchStatus());
        result.put("productionDate", batch.getProductionDate() != null ? batch.getProductionDate().format(DATE_FORMATTER) : null);
        result.put("expectedHarvestDate", batch.getExpectedHarvestDate() != null ? batch.getExpectedHarvestDate().format(DATE_FORMATTER) : null);
        result.put("actualHarvestDate", batch.getActualHarvestDate() != null ? batch.getActualHarvestDate().format(DATE_FORMATTER) : null);
        result.put("qrCodeUrl", batch.getQrCodeUrl());
        result.put("txHash", batch.getTxHash());
        result.put("dataHash", batch.getDataHash());
        result.put("blockNumber", batch.getBlockNumber());
        result.put("chainTime", batch.getChainTime() != null ? batch.getChainTime().format(DATETIME_FORMATTER) : null);
        result.put("createTime", batch.getCreateTime() != null ? batch.getCreateTime().format(DATETIME_FORMATTER) : null);
        result.put("remark", batch.getRemark());

        // 企业信息
        if (enterprise != null) {
            result.put("enterpriseId", enterprise.getId());
            result.put("enterpriseName", enterprise.getEnterpriseName());
            result.put("enterpriseType", enterprise.getEnterpriseType());
        } else {
            result.put("enterpriseId", batch.getEnterpriseId());
            result.put("enterpriseName", "");
            result.put("enterpriseType", null);
        }

        // 检疫信息
        if (inspection != null) {
            result.put("inspectionResult", inspection.getInspectionResult());
            result.put("inspectionDate", inspection.getInspectionDate() != null ? inspection.getInspectionDate().format(DATE_FORMATTER) : null);
            result.put("inspector", inspection.getInspector());
            result.put("certificateNo", inspection.getInspectionCertificateNo());
        }

        // 接收人和接收日期
        result.put("receiver", batch.getReceiver());
        result.put("receiveDate", batch.getReceiveDate() != null ? batch.getReceiveDate().format(DATE_FORMATTER) : null);

        // 查询最新加工记录的产出数量（供仓储录入页自动填入）
        LambdaQueryWrapper<TraceProcessing> processingWrapper = new LambdaQueryWrapper<>();
        processingWrapper.eq(TraceProcessing::getBatchId, batch.getId());
        processingWrapper.orderByDesc(TraceProcessing::getCreateTime);
        processingWrapper.last("LIMIT 1");
        TraceProcessing latestProcessing = traceProcessingMapper.selectOne(processingWrapper);
        if (latestProcessing != null) {
            result.put("processingOutputQuantity", latestProcessing.getOutputQuantity());
            result.put("processMethod", latestProcessing.getProcessMethod());
            result.put("processingDate", latestProcessing.getProcessingDate() != null ? latestProcessing.getProcessingDate().format(DATE_FORMATTER) : null);
        }

        return result;
    }

    /**
     * 删除批次（仅允许删除初始化状态的批次）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long id) {
        TraceBatch batch = traceBatchMapper.selectById(id);
        if (batch == null) {
            throw new RuntimeException("批次不存在");
        }
        // 允许删除状态1（初始化）和状态4（加工中，刚接收未入库）的批次
        if (batch.getBatchStatus() != 1 && batch.getBatchStatus() != 4) {
            throw new RuntimeException("仅允许删除初始化或刚接收的批次，当前状态: " + statusName(batch.getBatchStatus()));
        }
        // 软删除关联的生长记录（如果有），物理文件由定时任务7天后清理
        LambdaQueryWrapper<TraceRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(TraceRecord::getBatchId, id);
        java.util.List<TraceRecord> records = traceRecordMapper.selectList(recordWrapper);
        for (TraceRecord record : records) {
            traceRecordMapper.softDeleteById(record.getId());
        }
        traceBatchMapper.deleteById(id);
        log.info("批次已删除, id={}, batchCode={}, 关联记录{}条", id, batch.getBatchCode(), records.size());
    }

    /**
     * 取消接收批次（恢复状态为3）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelReceive(Long batchId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在");
        }
        if (batch.getBatchStatus() != 4) {
            throw new RuntimeException("只能取消加工中的批次，当前状态: " + statusName(batch.getBatchStatus()));
        }
        batch.setBatchStatus(3);
        batch.setUpdateTime(LocalDateTime.now());
        traceBatchMapper.updateById(batch);
        log.info("批次接收已取消, batchId={}, 状态恢复为3", batchId);
    }

    /**
     * 拒绝接收批次 - 回滚批次状态并通知养殖企业
     * @param batchId 批次ID
     * @param reason 拒绝原因
     * @param rejectEnterpriseId 拒绝方（加工企业）ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectBatch(Long batchId, String reason, Long rejectEnterpriseId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在");
        }
        // 仅允许拒绝运输中(7)的批次
        if (batch.getBatchStatus() != 7) {
            throw new RuntimeException("仅运输中的批次可以拒绝，当前状态: " + statusName(batch.getBatchStatus()));
        }

        // 回滚批次状态为已入库(6)
        batch.setBatchStatus(6);
        batch.setUpdateTime(LocalDateTime.now());
        traceBatchMapper.updateById(batch);

        // 删除指向该加工企业的运输记录
        LambdaQueryWrapper<TraceTransport> transportWrapper = new LambdaQueryWrapper<>();
        transportWrapper.eq(TraceTransport::getBatchId, batchId);
        transportWrapper.eq(TraceTransport::getReceiveEnterpriseId, rejectEnterpriseId);
        traceTransportMapper.delete(transportWrapper);

        // 获取加工企业名称
        String processingEnterpriseName = "";
        if (rejectEnterpriseId != null) {
            EnterpriseInfo processingEnterprise = enterpriseInfoMapper.selectById(rejectEnterpriseId);
            if (processingEnterprise != null) {
                processingEnterpriseName = processingEnterprise.getEnterpriseName();
            }
        }

        // 向养殖企业发送业务提醒
        Long farmingEnterpriseId = batch.getEnterpriseId();
        if (farmingEnterpriseId != null) {
            String title = "批次接收被拒绝";
            String summary = processingEnterpriseName + " 拒绝接收批次 " + batch.getBatchCode();
            String content = "批次编号: " + batch.getBatchCode()
                    + "\n产品名称: " + batch.getProductName()
                    + "\n拒绝原因: " + reason
                    + "\n拒绝企业: " + processingEnterpriseName
                    + "\n批次状态已回滚，请重新安排运输。";
            messageService.sendToEnterprise(farmingEnterpriseId, "business", title, summary, content, null, null, null);
        }

        log.info("批次 {} 被企业 {} 拒绝接收, 原因: {}", batch.getBatchCode(), rejectEnterpriseId, reason);
    }

    /**
     * 根据批次编号查询批次信息及检疫状态
     * @param batchCode 批次编号
     * @return 包含批次信息和检疫状态的Map
     */
    public Map<String, Object> checkQuarantine(String batchCode) {
        // 1. 根据 batchCode 查询批次
        LambdaQueryWrapper<TraceBatch> batchWrapper = new LambdaQueryWrapper<>();
        batchWrapper.eq(TraceBatch::getBatchCode, batchCode);
        TraceBatch batch = traceBatchMapper.selectOne(batchWrapper);

        if (batch == null) {
            throw new RuntimeException("未找到该批次: " + batchCode);
        }

        // 2. 查询该批次是否存在检疫合格记录 (check_result = 1)
        LambdaQueryWrapper<TraceInspection> inspectionWrapper = new LambdaQueryWrapper<>();
        inspectionWrapper.eq(TraceInspection::getBatchId, batch.getId());
        inspectionWrapper.eq(TraceInspection::getCheckResult, 1);
        inspectionWrapper.orderByDesc(TraceInspection::getInspectionDate);
        inspectionWrapper.last("LIMIT 1");
        TraceInspection passedInspection = traceInspectionMapper.selectOne(inspectionWrapper);

        // 3. 查询企业信息
        EnterpriseInfo enterprise = null;
        if (batch.getEnterpriseId() != null) {
            enterprise = enterpriseInfoMapper.selectById(batch.getEnterpriseId());
        }

        // 4. 组装返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batch.getId());
        result.put("batchCode", batch.getBatchCode());
        result.put("productName", batch.getProductName());
        result.put("productType", batch.getProductType());
        result.put("currentQuantity", batch.getCurrentQuantity());
        result.put("initQuantity", batch.getInitQuantity());
        result.put("unit", batch.getUnit());
        result.put("batchStatus", batch.getBatchStatus());
        result.put("enterpriseName", enterprise != null ? enterprise.getEnterpriseName() : "");

        if (passedInspection != null) {
            result.put("quarantinePassed", true);
            result.put("certificateNo", passedInspection.getInspectionCertificateNo());
            result.put("inspectionDate", passedInspection.getInspectionDate() != null
                    ? passedInspection.getInspectionDate().format(DATE_FORMATTER) : "");
            result.put("inspector", passedInspection.getInspector());
        } else {
            result.put("quarantinePassed", false);
        }

        return result;
    }

    /**
     * 接收批次 - 校验状态+检疫, 更新状态为4, 生成入库记录, 上链
     * @param dto 接收数据
     * @return 接收结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> receiveBatch(BatchReceiveDTO dto) {
        // 1. 查询批次
        LambdaQueryWrapper<TraceBatch> batchWrapper = new LambdaQueryWrapper<>();
        batchWrapper.eq(TraceBatch::getBatchCode, dto.getBatchCode());
        TraceBatch batch = traceBatchMapper.selectOne(batchWrapper);

        if (batch == null) {
            throw new RuntimeException("未找到该批次: " + dto.getBatchCode());
        }

        // 2. 校验批次状态 (必须为 3-已收获, 5-已检疫, 6-已入库, 或 7-运输中)
        if (batch.getBatchStatus() != 3 && batch.getBatchStatus() != 5 && batch.getBatchStatus() != 6 && batch.getBatchStatus() != 7) {
            throw new RuntimeException("该批次当前状态不允许接收（当前状态: " + statusName(batch.getBatchStatus()) + "）");
        }

        // 3. 校验检疫合格
        LambdaQueryWrapper<TraceInspection> inspectionWrapper = new LambdaQueryWrapper<>();
        inspectionWrapper.eq(TraceInspection::getBatchId, batch.getId());
        inspectionWrapper.eq(TraceInspection::getCheckResult, 1);
        Long passedCount = traceInspectionMapper.selectCount(inspectionWrapper);

        if (passedCount == 0) {
            throw new RuntimeException("该批次尚未取得检疫合格证，禁止接收");
        }

        // 4. 更新批次状态为"加工中"(4)，同时保存接收人、接收日期和接收企业ID
        batch.setBatchStatus(4);
        batch.setReceiver(dto.getReceiver());
        if (dto.getEnterpriseId() != null) {
            batch.setReceiveEnterpriseId(dto.getEnterpriseId());
        }
        if (StringUtils.hasText(dto.getReceiveDate())) {
            batch.setReceiveDate(java.time.LocalDate.parse(dto.getReceiveDate()));
        } else {
            batch.setReceiveDate(java.time.LocalDate.now());
        }
        batch.setUpdateTime(LocalDateTime.now());
        traceBatchMapper.updateById(batch);

        log.info("批次 {} 已接收, 状态更新为加工中(4), 接收人: {}, 接收数量: {}",
                dto.getBatchCode(), dto.getReceiver(), dto.getReceiveQuantity());

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("batchCode", batch.getBatchCode());
        result.put("batchStatus", batch.getBatchStatus());
        result.put("receiveQuantity", dto.getReceiveQuantity());
        result.put("receiver", dto.getReceiver());
        result.put("receiveDate", batch.getReceiveDate().toString());
        return result;
    }

    /**
     * 生成数据摘要 (简易SHA-256)
     */
    private String generateDataHash(String... parts) {
        try {
            String raw = String.join("|", parts) + "|" + System.currentTimeMillis();
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "hash_error_" + System.currentTimeMillis();
        }
    }

    /**
     * 实体转 VO
     */
    private BatchVO convertToVO(TraceBatch batch, EnterpriseInfo enterprise) {
        BatchVO vo = new BatchVO();
        vo.setId(batch.getId());
        vo.setBatchCode(batch.getBatchCode());
        vo.setProductName(batch.getProductName());
        vo.setProductType(batch.getProductType());
        vo.setBreed(batch.getBreed());
        vo.setInitQuantity(batch.getInitQuantity());
        vo.setQuantity(batch.getCurrentQuantity() != null ? batch.getCurrentQuantity() : batch.getInitQuantity());
        vo.setUnit(batch.getUnit());
        vo.setEnterpriseId(batch.getEnterpriseId());
        vo.setStatus(batch.getBatchStatus());
        vo.setTxHash(batch.getTxHash());
        vo.setDataHash(batch.getDataHash());
        vo.setQrCodeUrl(batch.getQrCodeUrl());
        vo.setQuarantinePassed(hasPassedInspection(batch.getId()));

        // 企业信息
        if (enterprise != null) {
            vo.setEnterpriseName(enterprise.getEnterpriseName());
            vo.setEnterpriseType(enterprise.getEnterpriseType());
        }

        // 格式化时间
        if (batch.getCreateTime() != null) {
            vo.setCreateTime(batch.getCreateTime().format(DATETIME_FORMATTER));
            vo.setCreateDate(batch.getCreateTime().format(DATE_FORMATTER));
        }
        if (batch.getUpdateTime() != null) {
            vo.setUpdateTime(batch.getUpdateTime().format(DATETIME_FORMATTER));
        }

        // 接收人和接收日期（直接存储在 trace_batch 表中）
        vo.setReceiver(batch.getReceiver());
        if (batch.getReceiveDate() != null) {
            vo.setReceiveDate(batch.getReceiveDate().format(DATE_FORMATTER));
        }

        return vo;
    }

    /** 空字符串 / null 转 null */
    private boolean hasPassedInspection(Long batchId) {
        if (batchId == null) {
            return false;
        }
        LambdaQueryWrapper<TraceInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TraceInspection::getBatchId, batchId);
        wrapper.eq(TraceInspection::getCheckResult, 1);
        return traceInspectionMapper.selectCount(wrapper) > 0;
    }

    private String toNullableString(Object val) {
        if (val == null) return null;
        String s = val.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /** 空字符串 / null 转 null, 有值则转 BigDecimal */
    private java.math.BigDecimal toNullableDecimal(Object val) {
        if (val == null) return null;
        String s = val.toString().trim();
        if (s.isEmpty()) return null;
        return new java.math.BigDecimal(s);
    }

    private void notifyAdmins(String title, String summary, String content, String actionUrl) {
        messageService.sendToAdmins("business", title, summary, content, actionUrl, "查看详情", "前往处理");
    }

    private String buildEnterprisePrefix(Long enterpriseId) {
        return "企业「" + resolveEnterpriseName(enterpriseId) + "」";
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

    /** 批次状态数字 -> 中文名称 */
    public static String statusName(Integer status) {
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
            default: return "未知(" + status + ")";
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
}
