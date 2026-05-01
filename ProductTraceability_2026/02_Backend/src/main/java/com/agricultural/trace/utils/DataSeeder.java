package com.agricultural.trace.utils;

import com.agricultural.trace.entity.*;
import com.agricultural.trace.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Random;

/**
 * 肉鸡全生命周期数据注入脚本
 * 生成 100 个批次 x 约 20 条记录 = ~2000 条溯源数据
 *
 * 启动条件: Spring Profile = "seed"
 * 运行方式: --spring.profiles.active=seed
 *
 * 关键设计：
 *   每条记录先 insert 拿到数据库自增 ID，
 *   再用与各 Service 完全一致的 traceId 格式和 dataHash 公式上链，
 *   最后 updateById 回写 txHash/blockNumber/dataHash/chainTime。
 *   这样验真时 TraceService.verifyBlockchain 用相同 traceId 查链，
 *   再比对 DB 中的 dataHash，两者完全吻合。
 */
@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TraceBatchMapper      batchMapper;
    private final TraceRecordMapper     recordMapper;
    private final TraceInspectionMapper inspectionMapper;
    private final TraceStorageMapper    storageMapper;
    private final TraceTransportMapper  transportMapper;
    private final TraceProcessingMapper processingMapper;
    private final TraceSaleMapper       saleMapper;
    private final Web3jUtils            web3jUtils;

    private static final Random RNG = new Random();

    private static final String[] BREEDS   = {"白羽肉鸡", "三黄鸡", "黄羽肉鸡", "走地鸡"};
    private static final String[] FEEDS    = {"玉米颗粒料", "优质豆粕", "肉鸡专用配合饲料"};
    private static final String[] VACCINES = {"禽流感疫苗", "新城疫灭活疫苗", "支气管炎疫苗"};
    private static final String[] PROCESS_METHODS = {"整鸡", "分切", "去骨"};
    private static final String[] CHANNELS  = {"批发市场", "超市", "电商平台"};
    private static final String[] DESTINATIONS = {"北京新发地", "生鲜超市", "物美大卖场"};
    private static final String[] RECORD_TYPES = {"feeding", "vaccine", "inspect"};
    private static final String[] OPERATORS_FARM = {"张三", "李四", "王五", "赵六"};
    private static final String[] OPERATORS_PROC = {"陈一", "刘二", "周七", "吴八"};
    private static final String[] PLATE_NUMBERS  = {"京A12345", "京B67890", "冀C11111", "津D22222"};
    private static final String[] DRIVER_NAMES   = {"孙师傅", "钱师傅", "郑师傅", "冯师傅"};
    private static final String[] CERT_IMAGES = {
        "/uploads/certificate/sample/cert_sample_1.jpg",
        "/uploads/certificate/sample/cert_sample_2.jpg",
        "/uploads/certificate/sample/cert_sample_3.jpg",
        "/uploads/certificate/sample/cert_sample_1.jpg",
        "/uploads/certificate/sample/cert_sample_2.jpg"
    };

    private static final String[] RECORD_IMAGES = {
        "/uploads/record/sample/chicken_farm_1.jpg",
        "/uploads/record/sample/chicken_farm_2.jpg",
        "/uploads/record/sample/chicken_farm_3.jpg"
    };

    private static final String[] PROCESSING_IMAGES = {
        "/uploads/processing/sample/processing_1.jpg",
        "/uploads/processing/sample/processing_2.jpg"
    };

    private static final String[] STORAGE_IMAGES = {
        "/uploads/storage/sample/storage_farm_1.jpg",
        "/uploads/storage/sample/storage_farm_2.jpg",
        "/uploads/storage/sample/storage_proc_1.jpg",
        "/uploads/storage/sample/storage_proc_2.jpg"
    };

    private static final String[] TRANSPORT_IMAGES = {
        "/uploads/transport/sample/transport_1.jpg",
        "/uploads/transport/sample/transport_2.jpg"
    };

    private static final String[] SALE_IMAGES = {
        "/uploads/sale/sample/sale_voucher_1.jpg",
        "/uploads/sale/sample/sale_voucher_2.jpg"
    };

    @Override
    public void run(String... args) {
        log.info("DataSeeder 启动，开始注入 100 个肉鸡批次数据...");
        int successCount = 0;
        for (int i = 0; i < 100; i++) {
            try {
                seedBatch(i);
                successCount++;
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("第 {} 批次注入失败，跳过: {}", i + 1, e.getMessage());
            }
        }
        log.info("DataSeeder 完成，成功注入 {}/100 个批次", successCount);
    }

    private void seedBatch(int batchIndex) {
        LocalDateTime baseTime = LocalDateTime.now()
                .minusDays(90)
                .plusHours((long) batchIndex * 12);
        LocalDate baseDate = baseTime.toLocalDate();

        String batchCode = "BATCH" + baseDate.toString().replace("-", "")
                + baseTime.format(java.time.format.DateTimeFormatter.ofPattern("HHmmss"))
                + String.format("%04d", batchIndex + 1);
        String breed = BREEDS[RNG.nextInt(BREEDS.length)];
        int quantity = 500 + RNG.nextInt(1500);

        // ---- Step 1: 批次初始化 ----
        // traceId 格式: batchCode + "_INIT"  (与 BatchService 一致)
        // dataHash 公式: "BATCH_INIT|code=...|product=...|type=...|qty=...|enterprise=...|ts=..."
        TraceBatch batch = new TraceBatch();
        batch.setBatchCode(batchCode);
        batch.setProductName("肉鸡");
        batch.setProductType(1);
        batch.setBreed(breed);
        batch.setEnterpriseId(1L);
        batch.setInitQuantity(BigDecimal.valueOf(quantity));
        batch.setCurrentQuantity(BigDecimal.valueOf(quantity));
        batch.setUnit("只");
        batch.setOriginLocation("河北省保定市养殖基地");
        batch.setSeedSource("河北省保定市种禽场");
        batch.setLatitude(new BigDecimal("38.8736"));
        batch.setLongitude(new BigDecimal("115.4642"));
        batch.setManager(OPERATORS_FARM[RNG.nextInt(OPERATORS_FARM.length)]);
        batch.setProductionDate(baseDate);
        batch.setExpectedHarvestDate(baseDate.plusDays(42));
        batch.setBatchStatus(1);
        batch.setCreateBy(1L);
        batch.setCreateTime(baseTime);
        batch.setUpdateTime(baseTime);
        batchMapper.insert(batch);
        Long batchId = batch.getId();

        // 生成溯源二维码URL（与 BatchService.createBatch 保持一致）
        String qrCodeUrl = "http://api.domain.com/trace?code=" + batchCode;
        batch.setQrCodeUrl(qrCodeUrl);
        batchMapper.updateById(batch);

        // 批次 insert 后拿到 ID，再上链（traceId 与 BatchService 完全一致）
        long ts0 = System.currentTimeMillis();
        String batchDataHash = sha256("BATCH_INIT|code=" + batchCode
                + "|product=" + batch.getProductName()
                + "|type=" + batch.getProductType()
                + "|qty=" + batch.getInitQuantity()
                + "|enterprise=" + batch.getEnterpriseId()
                + "|ts=" + ts0);
        chainWriteBack(batch, batchMapper, "trace_batch", batchCode + "_INIT", batchDataHash);
        // chainWriteBack 内部已通过 UpdateWrapper 精确更新四个区块链字段，无需再 updateById

        // ---- Step 2: 生长记录 x12 ----
        // traceId 格式: batchCode + "_RECORD_" + record.getId()  (与 RecordService 一致)
        // dataHash 公式: "RECORD|batchId=...|type=...|item=...|date=...|id=...|ts=..."
        updateBatchStatus(batchId, 2, baseTime.plusHours(1));
        for (int r = 0; r < 12; r++) {
            LocalDateTime recTime = baseTime.plusDays(r * 3L + 1);
            String rType = RECORD_TYPES[RNG.nextInt(RECORD_TYPES.length)];
            String itemName;
            String desc;
            if ("feeding".equals(rType)) {
                itemName = FEEDS[RNG.nextInt(FEEDS.length)];
                desc = "日常喂养，投喂 " + itemName;
            } else if ("vaccine".equals(rType)) {
                itemName = VACCINES[RNG.nextInt(VACCINES.length)];
                desc = "防疫接种，使用 " + itemName;
            } else {
                itemName = "日常巡查";
                desc = "鸡舍巡查，鸡群状态良好";
            }
            TraceRecord rec = new TraceRecord();
            rec.setBatchId(batchId);
            rec.setBatchCode(batchCode);
            rec.setRecordType(rType);
            rec.setRecordDate(recTime.toLocalDate());
            rec.setItemName(itemName);
            rec.setAmount(BigDecimal.valueOf(10 + RNG.nextInt(90)));
            rec.setDescription(desc);
            rec.setOperator(OPERATORS_FARM[RNG.nextInt(OPERATORS_FARM.length)]);
            rec.setLocation("河北省保定市养殖基地鸡舍" + (RNG.nextInt(5) + 1) + "号");
            rec.setDeleted(0);
            rec.setImages("[\"" + RECORD_IMAGES[RNG.nextInt(RECORD_IMAGES.length)] + "\"]");
            rec.setCreateBy(1L);
            rec.setCreateTime(recTime);
            rec.setUpdateTime(recTime);
            recordMapper.insert(rec);
            // insert 后拿到 ID，再上链
            long tsR = System.currentTimeMillis();
            String recHash = sha256("RECORD|batchId=" + rec.getBatchId()
                    + "|type=" + rec.getRecordType()
                    + "|item=" + rec.getItemName()
                    + "|date=" + rec.getRecordDate()
                    + "|id=" + rec.getId()
                    + "|ts=" + tsR);
            chainWriteBack(rec, recordMapper, "trace_record", batchCode + "_RECORD_" + rec.getId(), recHash);
        }

        // ---- Step 3: 申请检疫 (Status 3) ----
        LocalDateTime applyTime = baseTime.plusDays(38);
        updateBatchStatus(batchId, 3, applyTime);

        // ---- Step 4: 检疫录入 ----
        // traceId 格式: batchCode + "_INSPECTION_" + inspection.getId()  (与 QuarantineService 一致)
        // dataHash 公式: "BATCH|id=...|code=...|product=...|enterprise=...|INSPECTION|id=...|date=...|result=...|certNo=...|inspector=...|items=...|images=...|ts=..."
        LocalDateTime inspTime = baseTime.plusDays(40);
        TraceInspection insp = new TraceInspection();
        insp.setBatchId(batchId);
        insp.setBatchCode(batchCode);
        insp.setInspectionDate(inspTime.toLocalDate());
        insp.setCheckResult(1);
        insp.setInspectionItems("禽流感抗体检测,沙门氏菌检测,药物残留检测");
        insp.setCertNo("CERT" + String.format("%06d", batchIndex + 1));
        insp.setCertImage("[\"" + CERT_IMAGES[RNG.nextInt(CERT_IMAGES.length)] + "\"]");
        insp.setInspector("检疫员" + (RNG.nextInt(5) + 1));
        insp.setInspectorCode("QY" + String.format("%03d", RNG.nextInt(100) + 1));
        insp.setInspectionEnterpriseId(3L);
        insp.setCreateBy(3L);
        insp.setCreateTime(inspTime);
        insp.setUpdateTime(inspTime);
        inspectionMapper.insert(insp);
        // insert 后拿到 ID，再上链（公式与 QuarantineService.buildInspectionChainHash 完全一致）
        long tsI = System.currentTimeMillis();
        String inspHash = sha256("BATCH|"
                + "id=" + batchId + "|"
                + "code=" + batchCode + "|"
                + "product=" + batch.getProductName() + "|"
                + "enterprise=" + batch.getEnterpriseId() + "|"
                + "INSPECTION|"
                + "id=" + insp.getId() + "|"
                + "date=" + insp.getInspectionDate() + "|"
                + "result=" + insp.getCheckResult() + "|"
                + "certNo=" + (insp.getCertNo() != null ? insp.getCertNo() : "") + "|"
                + "inspector=" + (insp.getInspector() != null ? insp.getInspector() : "") + "|"
                + "items=" + (insp.getInspectionItems() != null ? insp.getInspectionItems() : "") + "|"
                + "images=" + (insp.getCertImage() != null ? insp.getCertImage() : "") + "|"
                + "ts=" + tsI);
        chainWriteBack(insp, inspectionMapper, "trace_inspection",
                batchCode + "_INSPECTION_" + insp.getId(), inspHash);
        updateBatchStatus(batchId, 5, inspTime.plusHours(2));

        // ---- Step 5 & 6: 养殖场入库 + 运输 ----
        // traceId 格式: batchCode + "_STORAGE_" + storage.getId()  (与 StorageService 一致)
        // dataHash 公式: buildFullBatchHash = "BATCH|id=...|code=...|product=...|type=...|breed=...|qty=...|origin=...|enterprise=...|RECORDS[n]|R{...}|...|INSPECTIONS[n]|I{...}|...|STORAGE{...}|ts=..."
        LocalDateTime storTime1 = baseTime.plusDays(41);
        TraceStorage stor1 = buildStorage(batchId, batchCode, 1L, 1,
                storTime1.toLocalDate(), "养殖场成品仓", "河北省保定市",
                BigDecimal.valueOf(quantity), "只", "3", "65",
                OPERATORS_FARM[RNG.nextInt(OPERATORS_FARM.length)], storTime1);
        storageMapper.insert(stor1);
        String stor1Hash = buildStorageHash(batch, stor1, batchId);
        chainWriteBack(stor1, storageMapper, "trace_storage",
                batchCode + "_STORAGE_" + stor1.getId(), stor1Hash);
        updateBatchStatus(batchId, 6, storTime1.plusHours(1));

        // traceId 格式: batchCode + "_TRANSPORT_" + transport.getId()  (与 TransportService 一致)
        LocalDateTime transTime1 = baseTime.plusDays(42);
        TraceTransport trans1 = buildTransport(batchId, batchCode, 1L, 2L,
                transTime1.toLocalDate(),
                PLATE_NUMBERS[RNG.nextInt(PLATE_NUMBERS.length)],
                DRIVER_NAMES[RNG.nextInt(DRIVER_NAMES.length)],
                "河北省保定市养殖基地", "北京市顺义区加工厂",
                transTime1, transTime1.plusHours(4),
                BigDecimal.valueOf(quantity), "只", transTime1);
        transportMapper.insert(trans1);
        String trans1Hash = buildTransportHash(trans1, System.currentTimeMillis());
        chainWriteBack(trans1, transportMapper, "trace_transport",
                batchCode + "_TRANSPORT_" + trans1.getId(), trans1Hash);
        updateBatchStatus(batchId, 7, transTime1.plusHours(1));

        // ---- Step 7: 加工厂接收 (Status 4) ----
        LocalDateTime recvTime = transTime1.plusHours(5);
        updateBatchReceiver(batchId, "加工厂收货员", recvTime.toLocalDate(), 2L, recvTime);
        updateBatchStatus(batchId, 4, recvTime);

        // ---- Step 8: 加工录入 ----
        // traceId 格式: sourceBatchCode + "_PROC_" + processing.getId()  (与 ProcessingService 一致)
        // dataHash 公式: "PROCESSING|batchId=...|method=...|specs=...|in=...|out=...|id=...|ts=..."
        LocalDateTime procTime = recvTime.plusDays(1);
        int outputQty = (int) (quantity * 0.85);
        TraceProcessing proc = new TraceProcessing();
        proc.setBatchId(batchId);
        proc.setSourceBatchCode(batchCode);
        proc.setProcessingEnterpriseId(2L);
        proc.setProcessingDate(procTime.toLocalDate());
        proc.setProcessMethod(PROCESS_METHODS[RNG.nextInt(PROCESS_METHODS.length)]);
        proc.setSpecs("500g/袋");
        proc.setOperator(OPERATORS_PROC[RNG.nextInt(OPERATORS_PROC.length)]);
        proc.setInputQuantity(BigDecimal.valueOf(quantity));
        proc.setInputUnit("只");
        proc.setOutputQuantity(BigDecimal.valueOf(outputQty));
        proc.setOutputUnit("kg");
        proc.setImages("[\"" + PROCESSING_IMAGES[RNG.nextInt(PROCESSING_IMAGES.length)] + "\"]");
        proc.setCreateBy(2L);
        proc.setCreateTime(procTime);
        proc.setUpdateTime(procTime);
        processingMapper.insert(proc);
        long tsP = System.currentTimeMillis();
        String procHash = sha256("PROCESSING|batchId=" + proc.getBatchId()
                + "|method=" + proc.getProcessMethod()
                + "|specs=" + proc.getSpecs()
                + "|in=" + proc.getInputQuantity()
                + "|out=" + proc.getOutputQuantity()
                + "|id=" + proc.getId()
                + "|ts=" + tsP);
        chainWriteBack(proc, processingMapper, "trace_processing",
                batchCode + "_PROC_" + proc.getId(), procHash);
        updateBatchStatus(batchId, 9, procTime.plusHours(2));

        // ---- Step 9: 加工厂成品入库 ----
        LocalDateTime storTime2 = procTime.plusHours(3);
        TraceStorage stor2 = buildStorage(batchId, batchCode, 2L, 1,
                storTime2.toLocalDate(), "加工厂冷链仓", "北京市顺义区",
                BigDecimal.valueOf(outputQty), "kg", "-2", "80",
                OPERATORS_PROC[RNG.nextInt(OPERATORS_PROC.length)], storTime2);
        storageMapper.insert(stor2);
        String stor2Hash = buildStorageHash(batch, stor2, batchId);
        chainWriteBack(stor2, storageMapper, "trace_storage",
                batchCode + "_STORAGE_" + stor2.getId(), stor2Hash);

        // ---- Step 10: 加工厂发货运输 ----
        LocalDateTime transTime2 = storTime2.plusDays(1);
        String destName = DESTINATIONS[RNG.nextInt(DESTINATIONS.length)];
        TraceTransport trans2 = buildTransport(batchId, batchCode, 2L, null,
                transTime2.toLocalDate(),
                PLATE_NUMBERS[RNG.nextInt(PLATE_NUMBERS.length)],
                DRIVER_NAMES[RNG.nextInt(DRIVER_NAMES.length)],
                "北京市顺义区加工厂", destName,
                transTime2, transTime2.plusHours(3),
                BigDecimal.valueOf(outputQty), "kg", transTime2);
        transportMapper.insert(trans2);
        String trans2Hash = buildTransportHash(trans2, System.currentTimeMillis());
        chainWriteBack(trans2, transportMapper, "trace_transport",
                batchCode + "_TRANSPORT_" + trans2.getId(), trans2Hash);

        // ---- Step 11: 最终销售 ----
        // traceId 格式: batchCode + "_SALE_" + sale.getId()  (与 SaleService 一致)
        // dataHash 公式: buildProcessingChainHash = "BATCH|...|PROCESSING[n]|P{...}|...|TRANSPORT[n]|T{...}|...|SALE{...}|ts=..."
        LocalDateTime saleTime = transTime2.plusHours(4);
        String channel = CHANNELS[RNG.nextInt(CHANNELS.length)];
        BigDecimal price = BigDecimal.valueOf(18 + RNG.nextInt(12));
        BigDecimal saleQty = BigDecimal.valueOf(outputQty);
        TraceSale sale = new TraceSale();
        sale.setBatchId(batchId);
        sale.setBatchCode(batchCode);
        sale.setSaleEnterpriseId(2L);
        sale.setSaleDate(saleTime.toLocalDate());
        sale.setSaleTime(LocalTime.of(saleTime.getHour(), saleTime.getMinute()));
        sale.setBuyerName(destName);
        sale.setSaleQuantity(saleQty);
        sale.setSaleUnit("kg");
        sale.setSalePrice(price);
        sale.setTotalAmount(price.multiply(saleQty));
        sale.setSaleChannel(channel);
        sale.setDestination(destName);
        sale.setSaleVoucher("[\"" + SALE_IMAGES[RNG.nextInt(SALE_IMAGES.length)] + "\"]");
        sale.setCreateBy(2L);
        sale.setCreateTime(saleTime);
        sale.setUpdateTime(saleTime);
        saleMapper.insert(sale);
        String saleHash = buildSaleHash(batch, proc, trans1, trans2, sale, batchId);
        chainWriteBack(sale, saleMapper, "trace_sale",
                batchCode + "_SALE_" + sale.getId(), saleHash);
        updateBatchStatus(batchId, 8, saleTime.plusHours(1));

        log.info("批次 [{}/100] {} 注入完成", batchIndex + 1, batchCode);
    }

    // ==================== 哈希构建方法（与各 Service 公式完全对齐）====================

    /**
     * 对应 StorageService.buildFullBatchHash
     * 公式: BATCH|id|code|product|type|breed|qty|origin|enterprise|RECORDS[n]|R{...}|INSPECTIONS[n]|I{...}|STORAGE{...}|ts
     */
    private String buildStorageHash(TraceBatch batch, TraceStorage storage, Long batchId) {
        StringBuilder sb = new StringBuilder();
        sb.append("BATCH|");
        sb.append("id=").append(batch.getId()).append("|");
        sb.append("code=").append(batch.getBatchCode()).append("|");
        sb.append("product=").append(batch.getProductName()).append("|");
        sb.append("type=").append(batch.getProductType()).append("|");
        sb.append("breed=").append(batch.getBreed()).append("|");
        sb.append("qty=").append(batch.getInitQuantity()).append("|");
        sb.append("origin=").append(batch.getOriginLocation()).append("|");
        sb.append("enterprise=").append(batch.getEnterpriseId()).append("|");

        java.util.List<TraceRecord> records = recordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TraceRecord>()
                        .eq(TraceRecord::getBatchId, batchId)
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

        java.util.List<TraceInspection> insps = inspectionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TraceInspection>()
                        .eq(TraceInspection::getBatchId, batchId)
                        .orderByAsc(TraceInspection::getInspectionDate));
        sb.append("INSPECTIONS[").append(insps.size()).append("]|");
        for (TraceInspection ins : insps) {
            sb.append("I{date=").append(ins.getInspectionDate())
              .append(",result=").append(ins.getCheckResult())
              .append(",certNo=").append(ins.getCertNo())
              .append(",inspector=").append(ins.getInspector())
              .append("}|");
        }

        sb.append("STORAGE{type=").append(storage.getStorageType())
          .append(",date=").append(storage.getStorageDate())
          .append(",warehouse=").append(storage.getWarehouseName())
          .append(",qty=").append(storage.getStorageQuantity())
          .append(",op=").append(storage.getOperator())
          .append("}|");
        sb.append("ts=").append(System.currentTimeMillis());
        return sha256(sb.toString());
    }

    /**
     * 对应 TransportService 哈希公式（完全一致）
     * 公式: TRANSPORT|batchId=...|plate=...|driver=...|from=...|to=...|id=...|ts=...
     */
    private String buildTransportHash(TraceTransport t, long ts) {
        String raw = "TRANSPORT|batchId=" + t.getBatchId()
                + "|plate=" + t.getPlateNumber()
                + "|driver=" + t.getDriverName()
                + "|from=" + t.getDepartureLocation()
                + "|to=" + t.getDestination()
                + "|id=" + t.getId()
                + "|ts=" + ts;
        return sha256(raw);
    }

    /**
     * 对应 SaleService.buildProcessingChainHash
     * 公式: BATCH|...|PROCESSING[n]|P{...}|TRANSPORT[n]|T{...}|SALE{...}|ts
     */
    private String buildSaleHash(TraceBatch batch, TraceProcessing proc,
                                  TraceTransport trans1, TraceTransport trans2,
                                  TraceSale sale, Long batchId) {
        StringBuilder sb = new StringBuilder();
        sb.append("BATCH|");
        sb.append("id=").append(batch.getId()).append("|");
        sb.append("code=").append(batch.getBatchCode()).append("|");
        sb.append("product=").append(batch.getProductName()).append("|");
        sb.append("type=").append(batch.getProductType()).append("|");
        sb.append("breed=").append(batch.getBreed()).append("|");
        sb.append("qty=").append(batch.getInitQuantity()).append("|");
        sb.append("enterprise=").append(batch.getEnterpriseId()).append("|");

        // 所有加工记录（与 SaleService 查库顺序一致）
        java.util.List<TraceProcessing> procList = processingMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TraceProcessing>()
                        .eq(TraceProcessing::getBatchId, batchId)
                        .orderByAsc(TraceProcessing::getProcessingDate));
        sb.append("PROCESSING[").append(procList.size()).append("]|");
        for (TraceProcessing p : procList) {
            sb.append("P{method=").append(p.getProcessMethod())
              .append(",date=").append(p.getProcessingDate())
              .append(",specs=").append(p.getSpecs())
              .append(",in=").append(p.getInputQuantity())
              .append(",out=").append(p.getOutputQuantity())
              .append(",op=").append(p.getOperator())
              .append("}|");
        }

        // 所有运输记录
        java.util.List<TraceTransport> transList = transportMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TraceTransport>()
                        .eq(TraceTransport::getBatchId, batchId)
                        .orderByAsc(TraceTransport::getTransportDate));
        sb.append("TRANSPORT[").append(transList.size()).append("]|");
        for (TraceTransport t : transList) {
            sb.append("T{date=").append(t.getTransportDate())
              .append(",plate=").append(t.getPlateNumber())
              .append(",driver=").append(t.getDriverName())
              .append(",from=").append(t.getDepartureLocation())
              .append(",to=").append(t.getDestination())
              .append(",qty=").append(t.getTransportQuantity())
              .append("}|");
        }

        sb.append("SALE{buyer=").append(sale.getBuyerName())
          .append(",date=").append(sale.getSaleDate())
          .append(",qty=").append(sale.getSaleQuantity())
          .append(",price=").append(sale.getSalePrice())
          .append(",channel=").append(sale.getSaleChannel())
          .append(",dest=").append(sale.getDestination())
          .append("}|");
        sb.append("ts=").append(System.currentTimeMillis());
        return sha256(sb.toString());
    }

    // ==================== 通用工具方法 ====================

    /**
     * 上链并通过 JdbcTemplate 直接 UPDATE 四个区块链字段，绕过 MyBatis-Plus 字段策略干扰。
     * 同时将值写入实体对象，供后续逻辑读取。
     * 调用时机：insert 之后，需要回写区块链信息时。
     *
     * @param entity    实体对象（需有 id 字段）
     * @param mapper    对应的 MyBatis-Plus BaseMapper
     * @param tableName 数据库表名
     * @param traceId   区块链 traceId
     * @param dataHash  SHA-256 哈希值
     */
    private void chainWriteBack(Object entity, com.baomidou.mybatisplus.core.mapper.BaseMapper mapper,
                                 String tableName, String traceId, String dataHash) {
        String txHash = null;
        Long blockNumber = 0L;
        LocalDateTime chainTime = LocalDateTime.now();

        try {
            txHash = web3jUtils.uploadHash(traceId, dataHash);
            TransactionReceipt receipt = web3jUtils.getTransactionReceipt(txHash);
            if (receipt != null && receipt.getBlockNumber() != null) {
                blockNumber = receipt.getBlockNumber().longValue();
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("already exists") || msg.contains("Trace ID already exists")) {
                // traceId 已存在于链上（重跑场景），dataHash 仍写入 DB 保持一致性
                // txHash 和 blockNumber 无法获取，置为占位值
                log.warn("traceId 已存在于链上，跳过上链，仅写入 dataHash: {}", traceId);
                txHash = "0x_duplicate_" + traceId.hashCode();
                blockNumber = 0L;
            } else {
                log.warn("区块链上传失败 [{}]: {}", traceId, msg);
                // 上链失败时仍写入 dataHash，保证 DB 字段不为 null
            }
        }

        setField(entity, "txHash",      txHash);
        setField(entity, "blockNumber", blockNumber);
        setField(entity, "chainTime",   chainTime);
        setField(entity, "dataHash",    dataHash);

        Long id = (Long) getField(entity, "id");
        if (id != null) {
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Object> uw =
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
            uw.eq("id", id)
              .set("tx_hash",      txHash)
              .set("block_number", blockNumber)
              .set("chain_time",   chainTime)
              .set("data_hash",    dataHash);
            try {
                mapper.update(null, uw);
            } catch (Exception dbEx) {
                log.warn("区块链字段回写 DB 失败 [{}]: {}", traceId, dbEx.getMessage());
            }
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = findField(obj.getClass(), fieldName);
            if (f != null) {
                f.setAccessible(true);
                f.set(obj, value);
            }
        } catch (Exception ignored) {}
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String name) {
        while (clazz != null && clazz != Object.class) {
            try { return clazz.getDeclaredField(name); }
            catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
        }
        return null;
    }

    private Object getField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field f = findField(obj.getClass(), fieldName);
            if (f != null) {
                f.setAccessible(true);
                return f.get(obj);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "hash_error_" + System.currentTimeMillis();
        }
    }

    private void updateBatchStatus(Long batchId, int status, LocalDateTime updateTime) {
        TraceBatch upd = new TraceBatch();
        upd.setId(batchId);
        upd.setBatchStatus(status);
        upd.setUpdateTime(updateTime);
        batchMapper.updateById(upd);
    }

    private void updateBatchReceiver(Long batchId, String receiver,
                                     LocalDate receiveDate, Long receiveEnterpriseId,
                                     LocalDateTime updateTime) {
        TraceBatch upd = new TraceBatch();
        upd.setId(batchId);
        upd.setReceiver(receiver);
        upd.setReceiveDate(receiveDate);
        upd.setReceiveEnterpriseId(receiveEnterpriseId);
        upd.setUpdateTime(updateTime);
        batchMapper.updateById(upd);
    }

    private TraceStorage buildStorage(Long batchId, String batchCode,
                                      Long enterpriseId, int storageType,
                                      LocalDate date, String warehouseName,
                                      String location, BigDecimal qty, String unit,
                                      String temp, String humidity,
                                      String operator, LocalDateTime createTime) {
        TraceStorage s = new TraceStorage();
        s.setBatchId(batchId);
        s.setBatchCode(batchCode);
        s.setStorageEnterpriseId(enterpriseId);
        s.setStorageType(storageType);
        s.setStorageDate(date);
        s.setWarehouseName(warehouseName);
        s.setWarehouseLocation(location);
        s.setStorageQuantity(qty);
        s.setStorageUnit(unit);
        s.setTemperature(new BigDecimal(temp));
        s.setHumidity(new BigDecimal(humidity));
        s.setStorageCondition("通风良好，温湿度达标");
        s.setOperator(operator);
        s.setImages("[\"" + STORAGE_IMAGES[RNG.nextInt(STORAGE_IMAGES.length)] + "\"]");
        s.setCreateBy(enterpriseId);
        s.setCreateTime(createTime);
        s.setUpdateTime(createTime);
        return s;
    }

    private TraceTransport buildTransport(Long batchId, String batchCode,
                                          Long transportEnterpriseId, Long receiveEnterpriseId,
                                          LocalDate date, String plate, String driver,
                                          String departure, String destination,
                                          LocalDateTime departureTime, LocalDateTime arrivalTime,
                                          BigDecimal qty, String unit, LocalDateTime createTime) {
        TraceTransport t = new TraceTransport();
        t.setBatchId(batchId);
        t.setBatchCode(batchCode);
        t.setLogisticsNo("LG" + System.currentTimeMillis() + RNG.nextInt(1000));
        t.setTransportEnterpriseId(transportEnterpriseId);
        t.setReceiveEnterpriseId(receiveEnterpriseId);
        t.setTransportDate(date);
        t.setPlateNumber(plate);
        t.setDriverName(driver);
        t.setDriverPhone("138" + String.format("%08d", RNG.nextInt(100000000)));
        t.setReceiverName("收货员" + (RNG.nextInt(5) + 1));
        t.setDepartureLocation(departure);
        t.setDestination(destination);
        t.setDepartureTime(departureTime);
        t.setArrivalTime(arrivalTime);
        t.setTransportQuantity(qty);
        t.setTransportUnit(unit);
        t.setTemperature(new BigDecimal("4.0"));
        t.setHumidity(new BigDecimal("70.0"));
        t.setTransportCondition("冷链运输，全程温控");
        t.setImages("[\"" + TRANSPORT_IMAGES[RNG.nextInt(TRANSPORT_IMAGES.length)] + "\"]");
        t.setCreateBy(transportEnterpriseId);
        t.setCreateTime(createTime);
        t.setUpdateTime(createTime);
        return t;
    }
}
