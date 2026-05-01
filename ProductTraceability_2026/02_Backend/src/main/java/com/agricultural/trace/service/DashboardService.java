package com.agricultural.trace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 首页看板服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M/d");

    /**
     * 养殖企业首页统计
     * @param enterpriseId 企业ID (可选, 为空则统计全部)
     */
    public Map<String, Object> getFarmerStats(Long enterpriseId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 在养批次数 (状态 1-初始化, 2-生长中)
        Long activeBatchCount = queryCount(
            "SELECT COUNT(*) FROM trace_batch WHERE batch_status IN (1,2)" + enterpriseWhere(enterpriseId));
        result.put("activeBatchCount", activeBatchCount);

        // 2. 存栏总数 (在养批次的 current_quantity 之和)
        Object totalObj = querySingle(
            "SELECT IFNULL(SUM(current_quantity),0) FROM trace_batch WHERE batch_status IN (1,2)" + enterpriseWhere(enterpriseId));
        result.put("totalLivestock", totalObj);

        // 3. 累计上链条数 (所有业务表中 tx_hash 不为空的总数，按批次所属企业过滤)
        String batchSub = enterpriseId != null
            ? " AND batch_id IN (SELECT id FROM trace_batch WHERE enterprise_id = " + enterpriseId + ")"
            : "";
        Long batchChain = queryCount(
            "SELECT COUNT(*) FROM trace_batch WHERE tx_hash IS NOT NULL AND tx_hash != ''" + enterpriseWhere(enterpriseId));
        Long recordChain = queryCount(
            "SELECT COUNT(*) FROM trace_record WHERE tx_hash IS NOT NULL AND tx_hash != ''" + batchSub);
        Long inspChain = queryCount(
            "SELECT COUNT(*) FROM trace_inspection WHERE tx_hash IS NOT NULL AND tx_hash != ''" + batchSub);
        Long storChain = queryCount(
            "SELECT COUNT(*) FROM trace_storage WHERE tx_hash IS NOT NULL AND tx_hash != ''" + batchSub);
        Long transChain = queryCount(
            "SELECT COUNT(*) FROM trace_transport WHERE tx_hash IS NOT NULL AND tx_hash != ''" + batchSub);
        Long procChain = queryCount(
            "SELECT COUNT(*) FROM trace_processing WHERE tx_hash IS NOT NULL AND tx_hash != ''" + batchSub);
        Long saleChain = queryCount(
            "SELECT COUNT(*) FROM trace_sale WHERE tx_hash IS NOT NULL AND tx_hash != ''" + batchSub);
        result.put("totalOnChain", batchChain + recordChain + inspChain + storChain + transChain + procChain + saleChain);

        // 4. 异常预警数 (超过预计收获日期仍未收获的批次)
        Long warningCount = queryCount(
            "SELECT COUNT(*) FROM trace_batch WHERE batch_status IN (1,2) AND expected_harvest_date IS NOT NULL AND expected_harvest_date < CURDATE()"
            + enterpriseWhere(enterpriseId));
        result.put("warningCount", warningCount);

        // 5. 批次总数
        Long batchCount = queryCount(
            "SELECT COUNT(*) FROM trace_batch" + enterpriseWhereOnly(enterpriseId));
        result.put("batchCount", batchCount);

        // 6. 记录总数（生长记录 + 检疫 + 仓储 + 运输）
        Long traceRecordCount = queryCount(
            "SELECT COUNT(*) FROM trace_record"
            + (enterpriseId != null ? " WHERE batch_id IN (SELECT id FROM trace_batch WHERE enterprise_id = " + enterpriseId + ")" : ""));
        Long inspectionCount = queryCount(
            "SELECT COUNT(*) FROM trace_inspection"
            + (enterpriseId != null ? " WHERE batch_id IN (SELECT id FROM trace_batch WHERE enterprise_id = " + enterpriseId + ")" : ""));
        Long storageCount = queryCount(
            "SELECT COUNT(*) FROM trace_storage"
            + (enterpriseId != null ? " WHERE batch_id IN (SELECT id FROM trace_batch WHERE enterprise_id = " + enterpriseId + ")" : ""));
        Long transportCount = queryCount(
            "SELECT COUNT(*) FROM trace_transport"
            + (enterpriseId != null ? " WHERE batch_id IN (SELECT id FROM trace_batch WHERE enterprise_id = " + enterpriseId + ")" : ""));
        result.put("recordCount", traceRecordCount + inspectionCount + storageCount + transportCount);

        // 7. 近7天生长记录录入趋势
        List<Map<String, Object>> dailyTrend = buildRecordTrend(enterpriseId);
        result.put("dailyTrend", dailyTrend);

        return result;
    }

    /**
     * 加工宰杀企业首页统计
     * @param enterpriseId 企业ID (可选)
     */
    public Map<String, Object> getProcessorStats(Long enterpriseId) {
        Map<String, Object> result = new HashMap<>();
        String today = LocalDate.now().toString();
        String entFilter = enterpriseId != null ? String.valueOf(enterpriseId) : null;

        // ============================================================
        // 加工企业的统计原则：只统计该企业自身操作产生的数据
        //   - 接收批次：trace_batch.receive_enterprise_id
        //   - 加工记录：trace_processing.processing_enterprise_id
        //   - 仓储记录：trace_storage.storage_enterprise_id
        //   - 运输记录：trace_transport.transport_enterprise_id
        //   - 销售记录：trace_sale.sale_enterprise_id
        // ============================================================

        // 1. 今日接收批次数
        if (entFilter != null) {
            result.put("todayReceived", queryCount(
                "SELECT COUNT(*) FROM trace_batch WHERE receive_enterprise_id = " + entFilter
                + " AND DATE(receive_date) = '" + today + "'"));
        } else {
            result.put("todayReceived", 0L);
        }

        // 2. 今日加工产出量
        String processSql = "SELECT IFNULL(SUM(output_quantity),0) FROM trace_processing WHERE DATE(processing_date) = '" + today + "'"
            + (entFilter != null ? " AND processing_enterprise_id = " + entFilter : "");
        result.put("todayProcessed", querySingle(processSql));

        // 3. 待销库存
        String inventorySql = "SELECT IFNULL(SUM(p.output_quantity),0) FROM trace_processing p "
            + "WHERE NOT EXISTS (SELECT 1 FROM trace_sale s WHERE s.batch_id = p.batch_id)"
            + (entFilter != null ? " AND p.processing_enterprise_id = " + entFilter : "");
        result.put("pendingSaleInventory", querySingle(inventorySql));

        // 4. 批次数（加工企业接收的批次）
        if (entFilter != null) {
            result.put("batchCount", queryCount(
                "SELECT COUNT(*) FROM trace_batch WHERE receive_enterprise_id = " + entFilter));
        } else {
            result.put("batchCount", queryCount("SELECT COUNT(*) FROM trace_batch"));
        }

        // 5. 记录数（加工企业各环节自身操作的记录总数）
        Long processingCount = queryCount(
            "SELECT COUNT(*) FROM trace_processing"
            + (entFilter != null ? " WHERE processing_enterprise_id = " + entFilter : ""));
        Long pStorageCount = queryCount(
            "SELECT COUNT(*) FROM trace_storage"
            + (entFilter != null ? " WHERE storage_enterprise_id = " + entFilter : ""));
        Long pTransportCount = queryCount(
            "SELECT COUNT(*) FROM trace_transport"
            + (entFilter != null ? " WHERE transport_enterprise_id = " + entFilter : ""));
        Long saleCount = queryCount(
            "SELECT COUNT(*) FROM trace_sale"
            + (entFilter != null ? " WHERE sale_enterprise_id = " + entFilter : ""));
        result.put("recordCount", processingCount + pStorageCount + pTransportCount + saleCount);

        // 6. 上链数（加工企业自身操作产生的上链记录）
        Long procChain = queryCount(
            "SELECT COUNT(*) FROM trace_processing WHERE tx_hash IS NOT NULL AND tx_hash != ''"
            + (entFilter != null ? " AND processing_enterprise_id = " + entFilter : ""));
        Long storageChain = queryCount(
            "SELECT COUNT(*) FROM trace_storage WHERE tx_hash IS NOT NULL AND tx_hash != ''"
            + (entFilter != null ? " AND storage_enterprise_id = " + entFilter : ""));
        Long transportChain = queryCount(
            "SELECT COUNT(*) FROM trace_transport WHERE tx_hash IS NOT NULL AND tx_hash != ''"
            + (entFilter != null ? " AND transport_enterprise_id = " + entFilter : ""));
        Long saleChain = queryCount(
            "SELECT COUNT(*) FROM trace_sale WHERE tx_hash IS NOT NULL AND tx_hash != ''"
            + (entFilter != null ? " AND sale_enterprise_id = " + entFilter : ""));
        result.put("totalOnChain", procChain + storageChain + transportChain + saleChain);

        // 7. 近7日加工量趋势
        result.put("processingTrend", buildProcessingTrend(enterpriseId));

        // 8. 原料转化率
        result.put("conversionRate", buildConversionRate(enterpriseId));

        // 9. 每日区块链存证笔数 (近30天)
        result.put("chainActivity", buildChainActivity(enterpriseId));

        return result;
    }

    /**
     * 近7日加工量趋势
     */
    private List<Map<String, Object>> buildProcessingTrend(Long enterpriseId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String sql = "SELECT IFNULL(SUM(output_quantity),0) FROM trace_processing WHERE DATE(processing_date) = ?"
                + (enterpriseId != null ? " AND processing_enterprise_id = " + enterpriseId : "");
            Object amount = 0;
            try {
                amount = jdbcTemplate.queryForObject(sql, Object.class, date.toString());
                if (amount == null) amount = 0;
            } catch (Exception e) {
                log.warn("processingTrend query error for {}: {}", date, e.getMessage());
            }
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(DATE_FMT));
            item.put("amount", amount);
            trend.add(item);
        }
        return trend;
    }

    /**
     * 原料转化率 (接收量 vs 产出量)
     */
    private List<Map<String, Object>> buildConversionRate(Long enterpriseId) {
        String entFilter = enterpriseId != null ? " AND processing_enterprise_id = " + enterpriseId : "";
        Object totalInput = querySingle(
            "SELECT IFNULL(SUM(input_quantity),0) FROM trace_processing WHERE 1=1" + entFilter);
        Object totalOutput = querySingle(
            "SELECT IFNULL(SUM(output_quantity),0) FROM trace_processing WHERE 1=1" + entFilter);
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> inputItem = new HashMap<>();
        inputItem.put("name", "接收量");
        inputItem.put("value", totalInput);
        list.add(inputItem);
        Map<String, Object> outputItem = new HashMap<>();
        outputItem.put("name", "产出量");
        outputItem.put("value", totalOutput);
        list.add(outputItem);
        return list;
    }

    /**
     * 检疫质检企业首页统计
     * @param enterpriseId 检疫企业ID
     */
    public Map<String, Object> getQuarantineStats(Long enterpriseId) {
        Map<String, Object> result = new HashMap<>();

        String inspFilter = enterpriseId != null
            ? " AND inspection_enterprise_id = " + enterpriseId : "";

        // 1. 检疫批次数（该检疫机构处理过的批次总数）
        Long batchCount = queryCount(
            "SELECT COUNT(DISTINCT batch_id) FROM trace_inspection WHERE 1=1" + inspFilter);
        result.put("batchCount", batchCount);

        // 2. 检疫记录数
        Long inspectionCount = queryCount(
            "SELECT COUNT(*) FROM trace_inspection WHERE 1=1" + inspFilter);
        result.put("inspectionCount", inspectionCount);

        // 3. 累计上链（该检疫机构的上链记录数）
        Long chainCount = queryCount(
            "SELECT COUNT(*) FROM trace_inspection WHERE tx_hash IS NOT NULL AND tx_hash != ''" + inspFilter);
        result.put("totalOnChain", chainCount);

        // 4. 待处理（已分配但尚未出结果的检疫，check_result IS NULL 或批次处于待检疫状态）
        Long pendingCount = queryCount(
            "SELECT COUNT(*) FROM trace_inspection WHERE check_result IS NULL" + inspFilter);
        result.put("pendingCount", pendingCount);

        // 5. 检疫合格率
        Long totalDone = queryCount(
            "SELECT COUNT(*) FROM trace_inspection WHERE check_result IS NOT NULL" + inspFilter);
        Long passCount = queryCount(
            "SELECT COUNT(*) FROM trace_inspection WHERE check_result = 1" + inspFilter);
        double passRate = totalDone > 0 ? Math.round(passCount * 10000.0 / totalDone) / 100.0 : 100.0;
        result.put("passRate", passRate);

        // 6. 近7天检疫记录趋势
        result.put("dailyTrend", buildQuarantineTrend(enterpriseId));

        // 7. 近30天每日上链笔数 (检疫企业)
        result.put("chainActivity", buildQuarantineChainActivity(enterpriseId));

        // 8. 检疫结果分布 (合格/不合格/待检)
        Long passedCount = queryCount(
            "SELECT COUNT(*) FROM trace_inspection WHERE check_result = 1" + inspFilter);
        Long failedCount = queryCount(
            "SELECT COUNT(*) FROM trace_inspection WHERE check_result = 0" + inspFilter);
        Long pendingInsp = queryCount(
            "SELECT COUNT(*) FROM trace_inspection WHERE check_result IS NULL" + inspFilter);
        List<Map<String, Object>> resultDist = new ArrayList<>();
        if (passedCount > 0) { Map<String, Object> m = new HashMap<>(); m.put("name", "合格"); m.put("value", passedCount); resultDist.add(m); }
        if (failedCount > 0) { Map<String, Object> m = new HashMap<>(); m.put("name", "不合格"); m.put("value", failedCount); resultDist.add(m); }
        if (pendingInsp > 0) { Map<String, Object> m = new HashMap<>(); m.put("name", "待检疫"); m.put("value", pendingInsp); resultDist.add(m); }
        result.put("resultDistribution", resultDist);

        // 9. 近30天检疫记录趋势（分析页用30天）
        result.put("inspectionTrend30", buildQuarantineTrend30(enterpriseId));

        return result;
    }

    /**
     * 近7天检疫记录录入趋势
     */
    private List<Map<String, Object>> buildQuarantineTrend(Long enterpriseId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String inspFilter = enterpriseId != null
            ? " AND inspection_enterprise_id = " + enterpriseId : "";

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String sql = "SELECT COUNT(*) FROM trace_inspection WHERE DATE(create_time) = ?" + inspFilter;
            Long count = 0L;
            try {
                count = jdbcTemplate.queryForObject(sql, Long.class, date.toString());
                if (count == null) count = 0L;
            } catch (Exception e) {
                log.warn("quarantineTrend query error for {}: {}", date, e.getMessage());
            }
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(DATE_FMT));
            item.put("count", count);
            trend.add(item);
        }
        return trend;
    }

    /**
     * 近30天每日上链笔数 (检疫企业, 基于 trace_inspection)
     */
    private List<Map<String, Object>> buildQuarantineChainActivity(Long enterpriseId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String inspFilter = enterpriseId != null
            ? " AND inspection_enterprise_id = " + enterpriseId : "";

        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.toString();
            Long count = queryCount(
                "SELECT COUNT(*) FROM trace_inspection WHERE DATE(chain_time) = '" + dateStr
                + "' AND tx_hash IS NOT NULL AND tx_hash != ''" + inspFilter);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            item.put("count", count);
            trend.add(item);
        }
        return trend;
    }

    /**
     * 近30天检疫记录录入趋势
     */
    private List<Map<String, Object>> buildQuarantineTrend30(Long enterpriseId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String inspFilter = enterpriseId != null
            ? " AND inspection_enterprise_id = " + enterpriseId : "";

        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String sql = "SELECT COUNT(*) FROM trace_inspection WHERE DATE(create_time) = ?" + inspFilter;
            Long count = 0L;
            try {
                count = jdbcTemplate.queryForObject(sql, Long.class, date.toString());
                if (count == null) count = 0L;
            } catch (Exception e) {
                log.warn("quarantineTrend30 query error for {}: {}", date, e.getMessage());
            }
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            item.put("amount", count);
            trend.add(item);
        }
        return trend;
    }

    /**
     * 数据统计分析页
     * 返回: activeBatchCount, totalLivestock, passRate,
     *       inputTrend(30天), statusDistribution, chainActivity(30天)
     */
    public Map<String, Object> getAnalysisStats(Long enterpriseId) {
        Map<String, Object> result = new HashMap<>();
        String ew = enterpriseWhere(enterpriseId);

        // 1. 在养批次 (状态 2-生长中)
        Long activeBatch = queryCount(
            "SELECT COUNT(*) FROM trace_batch WHERE batch_status IN (1,2)" + ew);
        result.put("activeBatchCount", activeBatch);

        // 2. 存栏总数 (所有在养批次 current_quantity 之和)
        Object totalLivestock = querySingle(
            "SELECT IFNULL(SUM(IFNULL(current_quantity, init_quantity)),0) FROM trace_batch WHERE batch_status IN (1,2)" + ew);
        result.put("totalLivestock", totalLivestock);

        // 3. 检疫合格率
        Long totalInspection = queryCount(
            "SELECT COUNT(*) FROM trace_inspection WHERE 1=1"
            + batchSubWhere(enterpriseId));
        Long passCount = queryCount(
            "SELECT COUNT(*) FROM trace_inspection WHERE check_result = 1"
            + batchSubWhere(enterpriseId));
        double passRate = totalInspection > 0 ? Math.round(passCount * 10000.0 / totalInspection) / 100.0 : 100.0;
        result.put("passRate", passRate);
        result.put("totalInspection", totalInspection);
        result.put("passCount", passCount);

        // 4. 近30天投入品使用统计 (trace_record 按日期聚合 amount)
        result.put("inputTrend", buildInputTrend(enterpriseId));

        // 5. 批次状态分布
        result.put("statusDistribution", buildStatusDistribution(enterpriseId));

        // 6. 每日区块链存证笔数 (近30天)
        result.put("chainActivity", buildChainActivity(enterpriseId));

        return result;
    }

    /**
     * 近30天投入品用量趋势
     */
    private List<Map<String, Object>> buildInputTrend(Long enterpriseId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String sub = batchSubWhere(enterpriseId);

        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String sql = "SELECT IFNULL(SUM(amount),0) FROM trace_record "
                    + "WHERE COALESCE(CASE WHEN record_date IS NULL OR record_date > CURDATE() "
                    + "THEN DATE(create_time) ELSE record_date END, DATE(create_time)) = ?"
                    + sub;
            Object amount = 0;
            try {
                amount = jdbcTemplate.queryForObject(sql, Object.class, date.toString());
                if (amount == null) amount = 0;
            } catch (Exception e) {
                log.warn("inputTrend query error for {}: {}", date, e.getMessage());
            }
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            item.put("amount", amount);
            trend.add(item);
        }
        return trend;
    }

    /**
     * 批次状态分布
     */
    private List<Map<String, Object>> buildStatusDistribution(Long enterpriseId) {
        String ew = enterpriseWhere(enterpriseId);
        String[][] groups = {
            {"养殖中", "1,2"},
            {"待检疫", "3"},
            {"加工中", "4"},
            {"已检疫", "5"},
            {"加工完成", "9"},
            {"已入库", "6"},
            {"运输中", "7"},
            {"已销售", "8"}
        };
        List<Map<String, Object>> list = new ArrayList<>();
        for (String[] g : groups) {
            Long count = queryCount(
                "SELECT COUNT(*) FROM trace_batch WHERE batch_status IN (" + g[1] + ")" + ew);
            if (count > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", g[0]);
                item.put("value", count);
                list.add(item);
            }
        }
        return list;
    }

    /**
     * 近30天每日上链笔数 (所有业务表 tx_hash 不为空, 按 chain_time 聚合)
     */
    private List<Map<String, Object>> buildChainActivity(Long enterpriseId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String ew = enterpriseWhere(enterpriseId);
        String sub = batchSubWhere(enterpriseId);

        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.toString();
            long total = 0;
            // trace_batch
            total += queryCount("SELECT COUNT(*) FROM trace_batch WHERE DATE(chain_time) = '" + dateStr + "' AND tx_hash IS NOT NULL AND tx_hash != ''" + ew);
            // trace_record
            total += queryCount("SELECT COUNT(*) FROM trace_record WHERE DATE(chain_time) = '" + dateStr + "' AND tx_hash IS NOT NULL AND tx_hash != ''" + sub);
            // trace_inspection
            total += queryCount("SELECT COUNT(*) FROM trace_inspection WHERE DATE(chain_time) = '" + dateStr + "' AND tx_hash IS NOT NULL AND tx_hash != ''" + sub);
            // trace_processing - 使用processing_enterprise_id
            String processingFilter = enterpriseId != null ? " AND processing_enterprise_id = " + enterpriseId : "";
            total += queryCount("SELECT COUNT(*) FROM trace_processing WHERE DATE(chain_time) = '" + dateStr + "' AND tx_hash IS NOT NULL AND tx_hash != ''" + processingFilter);
            // trace_storage - 使用storage_enterprise_id
            String storageFilter = enterpriseId != null ? " AND storage_enterprise_id = " + enterpriseId : "";
            total += queryCount("SELECT COUNT(*) FROM trace_storage WHERE DATE(chain_time) = '" + dateStr + "' AND tx_hash IS NOT NULL AND tx_hash != ''" + storageFilter);
            // trace_transport - 使用transport_enterprise_id
            String transportFilter = enterpriseId != null ? " AND transport_enterprise_id = " + enterpriseId : "";
            total += queryCount("SELECT COUNT(*) FROM trace_transport WHERE DATE(chain_time) = '" + dateStr + "' AND tx_hash IS NOT NULL AND tx_hash != ''" + transportFilter);
            // trace_sale - 使用sale_enterprise_id
            String saleFilter = enterpriseId != null ? " AND sale_enterprise_id = " + enterpriseId : "";
            total += queryCount("SELECT COUNT(*) FROM trace_sale WHERE DATE(chain_time) = '" + dateStr + "' AND tx_hash IS NOT NULL AND tx_hash != ''" + saleFilter);

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            item.put("count", total);
            trend.add(item);
        }
        return trend;
    }

    private String batchSubWhere(Long enterpriseId) {
        return enterpriseId != null
            ? " AND batch_id IN (SELECT id FROM trace_batch WHERE enterprise_id = " + enterpriseId + ")"
            : "";
    }

    /**
     * 近7天每日生长记录录入数量
     */
    private List<Map<String, Object>> buildRecordTrend(Long enterpriseId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateLabel = date.format(DATE_FMT);

            String sql = "SELECT COUNT(*) FROM trace_record WHERE DATE(create_time) = ?"
                + (enterpriseId != null
                    ? " AND batch_id IN (SELECT id FROM trace_batch WHERE enterprise_id = " + enterpriseId + ")"
                    : "");

            Long count = 0L;
            try {
                count = jdbcTemplate.queryForObject(sql, Long.class, date.toString());
                if (count == null) count = 0L;
            } catch (Exception e) {
                log.warn("查询 {} 记录数失败: {}", date, e.getMessage());
            }

            Map<String, Object> item = new HashMap<>();
            item.put("date", dateLabel);
            item.put("count", count);
            trend.add(item);
        }
        return trend;
    }

    /**
     * 管理员端统计大屏
     */
    public Map<String, Object> getAdminStats() {
        Map<String, Object> result = new HashMap<>();

        // 企业类型分布
        result.put("type1Count", queryCount("SELECT COUNT(*) FROM enterprise_info WHERE enterprise_type = 1 AND status = 1"));
        result.put("type2Count", queryCount("SELECT COUNT(*) FROM enterprise_info WHERE enterprise_type = 2 AND status = 1"));
        result.put("type3Count", queryCount("SELECT COUNT(*) FROM enterprise_info WHERE enterprise_type = 3 AND status = 1"));

        // 批次总数
        result.put("totalBatches", queryCount("SELECT COUNT(*) FROM trace_batch"));

        // 上链记录总数（汇总所有业务表）
        Long batchChain = queryCount("SELECT COUNT(*) FROM trace_batch WHERE tx_hash IS NOT NULL AND tx_hash != ''");
        Long recordChain = queryCount("SELECT COUNT(*) FROM trace_record WHERE tx_hash IS NOT NULL AND tx_hash != ''");
        Long inspChain = queryCount("SELECT COUNT(*) FROM trace_inspection WHERE tx_hash IS NOT NULL AND tx_hash != ''");
        Long storChain = queryCount("SELECT COUNT(*) FROM trace_storage WHERE tx_hash IS NOT NULL AND tx_hash != ''");
        Long transChain = queryCount("SELECT COUNT(*) FROM trace_transport WHERE tx_hash IS NOT NULL AND tx_hash != ''");
        Long procChain = queryCount("SELECT COUNT(*) FROM trace_processing WHERE tx_hash IS NOT NULL AND tx_hash != ''");
        Long saleChain = queryCount("SELECT COUNT(*) FROM trace_sale WHERE tx_hash IS NOT NULL AND tx_hash != ''");
        Long totalChain = batchChain + recordChain + inspChain + storChain + transChain + procChain + saleChain;
        
        log.info("上链统计: batch={}, record={}, insp={}, stor={}, trans={}, proc={}, sale={}, total={}", 
            batchChain, recordChain, inspChain, storChain, transChain, procChain, saleChain, totalChain);
        
        result.put("totalOnChain", totalChain);

        // 用户总数
        result.put("totalUsers", queryCount("SELECT COUNT(*) FROM sys_user WHERE status = 1"));

        // 批次状态分布
        List<Map<String, Object>> batchStatusCounts = new ArrayList<>();
        for (int s = 1; s <= 9; s++) {
            Long cnt = queryCount("SELECT COUNT(*) FROM trace_batch WHERE batch_status = " + s);
            if (cnt > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("status", s);
                item.put("count", cnt);
                batchStatusCounts.add(item);
            }
        }
        result.put("batchStatusCounts", batchStatusCounts);

        // 近12个月批次创建趋势
        List<Map<String, Object>> monthlyBatches = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String yearMonth = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String label = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            Long count = queryCount("SELECT COUNT(*) FROM trace_batch WHERE DATE_FORMAT(create_time, '%Y-%m') = '" + yearMonth + "'");
            Map<String, Object> item = new HashMap<>();
            item.put("month", label);
            item.put("count", count);
            monthlyBatches.add(item);
        }
        result.put("monthlyBatches", monthlyBatches);

        return result;
    }

    private String enterpriseWhere(Long enterpriseId) {
        return enterpriseId != null ? " AND enterprise_id = " + enterpriseId : "";
    }

    private String enterpriseWhereOnly(Long enterpriseId) {
        return enterpriseId != null ? " WHERE enterprise_id = " + enterpriseId : "";
    }

    private Long queryCount(String sql) {
        try {
            Long val = jdbcTemplate.queryForObject(sql, Long.class);
            return val != null ? val : 0L;
        } catch (Exception e) {
            log.warn("统计查询失败: {}", e.getMessage());
            return 0L;
        }
    }

    private Object querySingle(String sql) {
        try {
            return jdbcTemplate.queryForObject(sql, Object.class);
        } catch (Exception e) {
            log.warn("单值查询失败: {}", e.getMessage());
            return 0;
        }
    }
}
