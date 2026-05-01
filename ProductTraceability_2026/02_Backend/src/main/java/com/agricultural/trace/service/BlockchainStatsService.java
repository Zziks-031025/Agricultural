package com.agricultural.trace.service;

import com.agricultural.trace.config.BlockchainConfig;
import com.agricultural.trace.mapper.BlockchainGasFeeMapper;
import com.agricultural.trace.mapper.BlockchainTransactionMapper;
import com.agricultural.trace.mapper.TraceBatchMapper;
import com.agricultural.trace.mapper.TraceInspectionMapper;
import com.agricultural.trace.mapper.TraceRecordMapper;
import com.agricultural.trace.entity.BlockchainGasFee;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 区块链统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainStatsService {

    private static final String[] CORE_CHAIN_TABLES = {
            "trace_batch", "trace_record", "trace_inspection",
            "trace_processing", "trace_storage", "trace_transport", "trace_sale"
    };

    private final JdbcTemplate jdbcTemplate;
    private final BlockchainConfig blockchainConfig;
    private final BlockchainGasFeeMapper gasFeeMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M/d");

    /**
     * 获取区块链统计数据
     */
    public Map<String, Object> getStats(Long enterpriseId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 统计上链总量 (所有业务表中 tx_hash 不为空的记录数)
        long totalOnChain = enterpriseId != null ? countAllChainedByEnterprise(enterpriseId) : countAllChained();
        result.put("totalOnChain", totalOnChain);
        result.put("totalCount", totalOnChain);

        // 2. 今日上链
        long todayOnChain = enterpriseId != null ? countAllChainedTodayByEnterprise(enterpriseId) : countAllChainedToday();
        result.put("todayOnChain", todayOnChain);
        result.put("todayCount", todayOnChain);

        // 3. 本周上链
        long weekOnChain = enterpriseId != null ? countAllChainedThisWeekByEnterprise(enterpriseId) : countAllChainedThisWeek();
        result.put("weekCount", weekOnChain);

        // 4. 成功率 (有 tx_hash 视为成功)
        result.put("successRate", totalOnChain > 0 ? "100" : "0");

        // 5. 平均 Gas (基于配置估算)
        long avgGasPerTx = 200000L;
        long gasPriceGwei = blockchainConfig.getGasPrice() != null ? blockchainConfig.getGasPrice() : 20L;
        double avgGasWei = avgGasPerTx * gasPriceGwei * 1_000_000_000L;
        String avgGasDisplay = totalOnChain > 0 
            ? String.format("%.2f Gwei", avgGasWei / 1_000_000_000.0)
            : "--";
        result.put("avgGas", avgGasDisplay);

        // 6. 近7天每日上链趋势
        List<Map<String, Object>> dailyTrend = buildDailyTrend(enterpriseId);
        result.put("dailyTrend", dailyTrend);

        // 7. 各环节上链分布
        long batchChain = countChainedSafe("trace_batch", enterpriseId);
        long recordChain = countChainedSafe("trace_record", enterpriseId);
        long inspectionChain = countChainedSafe("trace_inspection", enterpriseId);
        long processChain = countChainedSafe("trace_processing", enterpriseId);
        long storageChain = countChainedSafe("trace_storage", enterpriseId);
        long transportChain = countChainedSafe("trace_transport", enterpriseId);
        long saleChain = countChainedSafe("trace_sale", enterpriseId);

        List<Map<String, Object>> stageDistribution = new ArrayList<>();
        stageDistribution.add(buildStageItem("批次初始化", batchChain, "#2d8a56"));
        stageDistribution.add(buildStageItem("生长记录", recordChain, "#3ca66b"));
        stageDistribution.add(buildStageItem("检疫质检", inspectionChain, "#52bf84"));
        stageDistribution.add(buildStageItem("加工环节", processChain, "#7dd4a0"));
        stageDistribution.add(buildStageItem("仓储入库", storageChain, "#a8e6bf"));
        stageDistribution.add(buildStageItem("物流运输", transportChain, "#c8f0d4"));
        stageDistribution.add(buildStageItem("销售环节", saleChain, "#e6a23c"));
        result.put("stageDistribution", stageDistribution);

        // 8. 真实 Gas 费用统计 (从 blockchain_gas_fee 表读取)
        Map<String, Object> gasStats = buildGasStats(enterpriseId);
        result.putAll(gasStats);

        return result;
    }

    private long countChained(String tableName) {
        try {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE tx_hash IS NOT NULL AND tx_hash != ''",
                Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("统计表 {} 上链数量失败: {}", tableName, e.getMessage());
            return 0;
        }
    }

    private long countChainedSafe(String tableName) {
        try {
            return countChained(tableName);
        } catch (Exception e) {
            return 0;
        }
    }

    private long countChainedSafe(String tableName, Long enterpriseId) {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tableName + " WHERE tx_hash IS NOT NULL AND tx_hash != ''"
                            + buildEnterpriseFilter(tableName, enterpriseId),
                    Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private long countChainedToday(String tableName) {
        try {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName +
                " WHERE tx_hash IS NOT NULL AND tx_hash != '' AND DATE(chain_time) = CURDATE()",
                Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("统计表 {} 今日上链失败: {}", tableName, e.getMessage());
            return 0;
        }
    }

    private List<Map<String, Object>> buildDailyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        long avgGasPerTx = 200000L;
        long gasPriceGwei = blockchainConfig.getGasPrice() != null ? blockchainConfig.getGasPrice() : 20L;

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FMT);
            long count = countChainedByDate(date);
            
            long dailyGasWei = count * avgGasPerTx * gasPriceGwei * 1_000_000_000L;

            Map<String, Object> item = new HashMap<>();
            item.put("date", dateStr);
            item.put("count", count);
            item.put("gas", dailyGasWei);
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> buildDailyTrend(Long enterpriseId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        long avgGasPerTx = 200000L;
        long gasPriceGwei = blockchainConfig.getGasPrice() != null ? blockchainConfig.getGasPrice() : 20L;

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FMT);
            long count = countChainedByDate(date, enterpriseId);

            long dailyGasWei = count * avgGasPerTx * gasPriceGwei * 1_000_000_000L;

            Map<String, Object> item = new HashMap<>();
            item.put("date", dateStr);
            item.put("count", count);
            item.put("gas", dailyGasWei);
            trend.add(item);
        }
        return trend;
    }

    private long countChainedByDate(LocalDate date) {
        long total = 0;
        String[] tables = {"trace_batch", "trace_record", "trace_inspection"};
        for (String table : tables) {
            try {
                Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table +
                    " WHERE tx_hash IS NOT NULL AND tx_hash != '' AND DATE(chain_time) = ?",
                    Long.class, date.toString());
                total += (count != null ? count : 0);
            } catch (Exception e) {
                // table may not have chain_time column
            }
        }
        return total;
    }

    private long countChainedByDate(LocalDate date, Long enterpriseId) {
        long total = 0;
        for (String table : CORE_CHAIN_TABLES) {
            try {
                Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + table
                                + " WHERE tx_hash IS NOT NULL AND tx_hash != '' AND DATE(chain_time) = ?"
                                + buildEnterpriseFilter(table, enterpriseId),
                        Long.class,
                        date.toString());
                total += (count != null ? count : 0);
            } catch (Exception e) {
                // table may not have chain_time column
            }
        }
        return total;
    }

    private Map<String, Object> buildStageItem(String name, long value, String color) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        Map<String, String> style = new HashMap<>();
        style.put("color", color);
        item.put("itemStyle", style);
        return item;
    }

    // ========== Logs & Retry ==========

    private static final Map<String, String> BIZ_TYPE_MAP = new LinkedHashMap<>();
    static {
        BIZ_TYPE_MAP.put("batch_init", "trace_batch");
        BIZ_TYPE_MAP.put("growth_record", "trace_record");
        BIZ_TYPE_MAP.put("quarantine", "trace_inspection");
        BIZ_TYPE_MAP.put("processing", "trace_processing");
        BIZ_TYPE_MAP.put("storage", "trace_storage");
        BIZ_TYPE_MAP.put("transport", "trace_transport");
        BIZ_TYPE_MAP.put("sale", "trace_sale");
    }

    /**
     * 查询上链日志列表 (从各业务表聚合, JOIN trace_batch 获取产品名/企业名)
     */
    public Map<String, Object> getLogs(int pageNum, int pageSize,
                                       Long enterpriseId, String bizType, Integer status,
                                       String txHash, String batchCode) {
        List<String> unions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, String> entry : BIZ_TYPE_MAP.entrySet()) {
            String type = entry.getKey();
            String table = entry.getValue();

            if (bizType != null && !bizType.isEmpty() && !bizType.equals(type)) {
                continue;
            }
            if (!tableExists(table)) continue;

            String entCol = getEnterpriseColumn(table);
            StringBuilder sb = new StringBuilder();

            if (table.equals("trace_batch")) {
                sb.append("SELECT t.id, t.tx_hash, t.block_number, '").append(type).append("' AS biz_type, ");
                sb.append("'init' AS record_type, ");
                sb.append("t.batch_code AS batch_code, ");
                sb.append("t.product_name AS product_name, ");
                sb.append("e.enterprise_name AS enterprise_name, ");
                sb.append("'' AS operator, ");
                sb.append("t.chain_time, t.id AS batch_id ");
                sb.append("FROM trace_batch t ");
                sb.append("LEFT JOIN enterprise_info e ON t.enterprise_id = e.id ");
                sb.append("WHERE t.tx_hash IS NOT NULL AND t.tx_hash != ''");
                if (enterpriseId != null) {
                    sb.append(" AND (t.enterprise_id = ? OR t.receive_enterprise_id = ?)");
                    params.add(enterpriseId);
                    params.add(enterpriseId);
                }
            } else if (table.equals("trace_record")) {
                // trace_record 自身有 record_type (feeding/vaccine/inspect等)
                sb.append("SELECT t.id, t.tx_hash, t.block_number, '").append(type).append("' AS biz_type, ");
                sb.append("t.record_type AS record_type, ");
                sb.append("b.batch_code AS batch_code, ");
                sb.append("b.product_name AS product_name, ");
                sb.append("e.enterprise_name AS enterprise_name, ");
                sb.append("t.operator AS operator, ");
                sb.append("t.chain_time, t.batch_id ");
                sb.append("FROM trace_record t ");
                sb.append("LEFT JOIN trace_batch b ON t.batch_id = b.id ");
                sb.append("LEFT JOIN enterprise_info e ON b.enterprise_id = e.id ");
                sb.append("WHERE t.tx_hash IS NOT NULL AND t.tx_hash != ''");
                if (enterpriseId != null) {
                    sb.append(" AND b.enterprise_id = ?");
                    params.add(enterpriseId);
                }
            } else {
                String operatorCol = getOperatorColumn(table);
                String fixedRecordType = getFixedRecordType(type);
                sb.append("SELECT t.id, t.tx_hash, t.block_number, '").append(type).append("' AS biz_type, ");
                sb.append("'").append(fixedRecordType).append("' AS record_type, ");
                sb.append("b.batch_code AS batch_code, ");
                sb.append("b.product_name AS product_name, ");
                sb.append("e.enterprise_name AS enterprise_name, ");
                sb.append(operatorCol).append(" AS operator, ");
                sb.append("t.chain_time, t.batch_id ");
                sb.append("FROM ").append(table).append(" t ");
                sb.append("LEFT JOIN trace_batch b ON t.batch_id = b.id ");
                sb.append("LEFT JOIN enterprise_info e ON b.enterprise_id = e.id ");
                sb.append("WHERE t.tx_hash IS NOT NULL AND t.tx_hash != ''");
                if (enterpriseId != null) {
                    if (table.equals("trace_inspection")) {
                        sb.append(" AND (b.enterprise_id = ? OR t.inspection_enterprise_id = ?)");
                        params.add(enterpriseId);
                        params.add(enterpriseId);
                    } else if (entCol != null) {
                        sb.append(" AND (t.").append(entCol).append(" = ? OR b.enterprise_id = ?)");
                        params.add(enterpriseId);
                        params.add(enterpriseId);
                    } else {
                        sb.append(" AND b.enterprise_id = ?");
                        params.add(enterpriseId);
                    }
                }
            }

            if (status != null) {
                if (status == 1) {
                    // already filtered by tx_hash not null
                } else {
                    continue;
                }
            }

            // 按txHash精确过滤
            if (txHash != null && !txHash.isEmpty()) {
                sb.append(" AND t.tx_hash = ?");
                params.add(txHash);
            }

            // 按batchCode过滤
            if (batchCode != null && !batchCode.isEmpty()) {
                if (table.equals("trace_batch")) {
                    sb.append(" AND t.batch_code = ?");
                } else {
                    sb.append(" AND b.batch_code = ?");
                }
                params.add(batchCode);
            }

            unions.add(sb.toString());
        }

        if (unions.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("records", Collections.emptyList());
            empty.put("total", 0);
            empty.put("totalChained", countAllChained());
            empty.put("todayChained", countAllChainedToday());
            empty.put("gasEstimate", "--");
            return empty;
        }

        String unionSql = String.join(" UNION ALL ", unions);

        // Count total
        String countSql = "SELECT COUNT(*) FROM (" + unionSql + ") AS u";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        // Paginated query
        int offset = (pageNum - 1) * pageSize;
        String pageSql = "SELECT * FROM (" + unionSql + ") AS u ORDER BY chain_time DESC LIMIT ? OFFSET ?";
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(pageSql, pageParams.toArray());

        // Map field names to match frontend expectations
        for (Map<String, Object> row : records) {
            Object bn = row.get("block_number");
            row.put("blockNumber", bn != null ? String.valueOf(bn) : null);
            row.put("blockHeight", bn != null ? String.valueOf(bn) : "--");
            row.put("bizType", row.get("biz_type"));
            row.put("batchCode", row.get("batch_code"));
            row.put("batchId", row.get("batch_id"));
            row.put("txHash", row.get("tx_hash"));
            row.put("productName", row.get("product_name"));
            row.put("enterpriseName", row.get("enterprise_name"));
            row.put("operator", row.get("operator"));
            // record_type 已在UNION查询中直接获取
            row.put("recordType", row.get("record_type"));
            Object ct = row.get("chain_time");
            row.put("chainTime", ct != null ? ct.toString() : "--");
            row.put("status", 1);
            row.put("contractAddress", blockchainConfig.getContractAddress());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total != null ? total : 0);
        long totalChained = enterpriseId != null ? countAllChainedByEnterprise(enterpriseId) : countAllChained();
        long todayChained = enterpriseId != null ? countAllChainedTodayByEnterprise(enterpriseId) : countAllChainedToday();
        result.put("totalChained", totalChained);
        result.put("todayChained", todayChained);

        // Gas消耗估算: 每笔交易平均约200000 gas, gasPrice从配置读取(Gwei)
        long avgGasPerTx = 200000L;
        long gasPriceGwei = blockchainConfig.getGasPrice() != null ? blockchainConfig.getGasPrice() : 20L;
        double totalGasWei = (double) totalChained * avgGasPerTx * gasPriceGwei * 1_000_000_000L;
        double totalGasEth = totalGasWei / 1e18;
        String gasEstimate = totalChained > 0
            ? String.format("%.4f ETH", totalGasEth)
            : "0 ETH";
        result.put("gasEstimate", gasEstimate);
        return result;
    }

    private String getOperatorColumn(String table) {
        switch (table) {
            case "trace_record": return "t.operator";
            case "trace_processing": return "t.operator";
            case "trace_storage": return "t.operator";
            case "trace_transport": return "t.driver_name";
            default: return "''";
        }
    }

    private String getFixedRecordType(String bizType) {
        if (bizType == null) return "init";
        switch (bizType) {
            case "batch_init": return "init";
            case "quarantine": return "quarantine";
            case "processing": return "processing";
            case "storage": return "storage";
            case "transport": return "transport";
            case "sale": return "sale";
            default: return bizType;
        }
    }

    /**
     * 重新上链 (placeholder - actual retry depends on Web3j integration)
     */
    public void retryChain(Long id) {
        log.info("重新上链请求, id={}", id);
        // In a real implementation, this would re-submit the transaction via Web3j.
        // For now, just log the attempt.
        throw new RuntimeException("重新上链功能需要区块链节点在线");
    }

    private long countAllChained() {
        long total = 0;
        for (String table : BIZ_TYPE_MAP.values()) {
            total += countChainedSafe(table);
        }
        return total;
    }

    private long countAllChainedToday() {
        long total = 0;
        String[] coreTables = {"trace_batch", "trace_record", "trace_inspection",
                "trace_processing", "trace_storage", "trace_transport", "trace_sale"};
        for (String table : coreTables) {
            try {
                Long c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table +
                    " WHERE tx_hash IS NOT NULL AND tx_hash != '' AND DATE(chain_time) = CURDATE()",
                    Long.class);
                total += (c != null ? c : 0);
            } catch (Exception e) {
                // ignore
            }
        }
        return total;
    }

    private long countAllChainedThisWeek() {
        long total = 0;
        String[] coreTables = {"trace_batch", "trace_record", "trace_inspection",
                "trace_processing", "trace_storage", "trace_transport", "trace_sale"};
        for (String table : coreTables) {
            try {
                Long c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table +
                    " WHERE tx_hash IS NOT NULL AND tx_hash != '' AND chain_time >= DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)",
                    Long.class);
                total += (c != null ? c : 0);
            } catch (Exception e) {
                // ignore
            }
        }
        return total;
    }

    /**
     * 构建企业过滤条件（按各表自身的企业ID字段过滤）
     */
    private String buildEnterpriseFilter(String table, Long enterpriseId) {
        if (enterpriseId == null) return "";
        switch (table) {
            case "trace_batch":
                return " AND (enterprise_id = " + enterpriseId + " OR receive_enterprise_id = " + enterpriseId + ")";
            case "trace_record":
                return " AND batch_id IN (SELECT id FROM trace_batch WHERE enterprise_id = " + enterpriseId + ")";
            case "trace_inspection":
                return " AND (batch_id IN (SELECT id FROM trace_batch WHERE enterprise_id = " + enterpriseId + ") OR inspection_enterprise_id = " + enterpriseId + ")";
            case "trace_processing":
                return " AND processing_enterprise_id = " + enterpriseId;
            case "trace_storage":
                return " AND storage_enterprise_id = " + enterpriseId;
            case "trace_transport":
                return " AND transport_enterprise_id = " + enterpriseId;
            case "trace_sale":
                return " AND sale_enterprise_id = " + enterpriseId;
            default:
                return "";
        }
    }

    private long countAllChainedByEnterprise(Long enterpriseId) {
        long total = 0;
        String[] tables = {"trace_batch", "trace_record", "trace_inspection",
                "trace_processing", "trace_storage", "trace_transport", "trace_sale"};
        for (String table : tables) {
            try {
                String extra = buildEnterpriseFilter(table, enterpriseId);
                Long c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE tx_hash IS NOT NULL AND tx_hash != ''" + extra,
                    Long.class);
                total += (c != null ? c : 0);
            } catch (Exception e) { /* ignore */ }
        }
        return total;
    }

    private long countAllChainedTodayByEnterprise(Long enterpriseId) {
        long total = 0;
        String[] tables = {"trace_batch", "trace_record", "trace_inspection",
                "trace_processing", "trace_storage", "trace_transport", "trace_sale"};
        for (String table : tables) {
            try {
                String extra = buildEnterpriseFilter(table, enterpriseId);
                Long c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE tx_hash IS NOT NULL AND tx_hash != '' AND DATE(chain_time) = CURDATE()" + extra,
                    Long.class);
                total += (c != null ? c : 0);
            } catch (Exception e) { /* ignore */ }
        }
        return total;
    }

    private long countAllChainedThisWeekByEnterprise(Long enterpriseId) {
        long total = 0;
        String[] tables = {"trace_batch", "trace_record", "trace_inspection",
                "trace_processing", "trace_storage", "trace_transport", "trace_sale"};
        for (String table : tables) {
            try {
                String extra = buildEnterpriseFilter(table, enterpriseId);
                Long c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE tx_hash IS NOT NULL AND tx_hash != '' AND chain_time >= DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)" + extra,
                    Long.class);
                total += (c != null ? c : 0);
            } catch (Exception e) { /* ignore */ }
        }
        return total;
    }

    private boolean tableExists(String tableName) {
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM " + tableName + " LIMIT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT " + columnName + " FROM " + tableName + " LIMIT 1", Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getEnterpriseColumn(String table) {
        switch (table) {
            case "trace_batch": return "enterprise_id";
            case "trace_processing": return "processing_enterprise_id";
            case "trace_storage": return "storage_enterprise_id";
            case "trace_transport": return "transport_enterprise_id";
            case "trace_sale": return "sale_enterprise_id";
            case "trace_inspection": return "inspection_enterprise_id";
            default: return null;
        }
    }

    /**
     * 从 blockchain_gas_fee 表构建真实 Gas 费用统计
     */
    private Map<String, Object> buildGasStats(Long enterpriseId) {
        Map<String, Object> gas = new HashMap<>();
        try {
            // 总费用
            LambdaQueryWrapper<BlockchainGasFee> totalWrapper = new LambdaQueryWrapper<>();
            if (enterpriseId != null) {
                totalWrapper.eq(BlockchainGasFee::getEnterpriseId, enterpriseId);
            } else {
                totalWrapper.isNull(BlockchainGasFee::getEnterpriseId);
            }
            List<BlockchainGasFee> allFees = gasFeeMapper.selectList(totalWrapper);

            BigDecimal totalFeeEth = BigDecimal.ZERO;
            BigDecimal todayFeeEth = BigDecimal.ZERO;
            long totalAvgGasPrice = 0;
            int count = 0;

            LocalDate today = LocalDate.now();
            for (BlockchainGasFee fee : allFees) {
                if (fee.getTotalFeeEth() != null) {
                    totalFeeEth = totalFeeEth.add(fee.getTotalFeeEth());
                }
                if (fee.getAvgGasPrice() != null) {
                    totalAvgGasPrice += fee.getAvgGasPrice();
                    count++;
                }
                if (today.equals(fee.getStatDate()) && fee.getTotalFeeEth() != null) {
                    todayFeeEth = todayFeeEth.add(fee.getTotalFeeEth());
                }
            }

            gas.put("totalFeeEth", totalFeeEth.toPlainString());
            gas.put("todayFeeEth", todayFeeEth.toPlainString());
            gas.put("avgGasPrice", count > 0 ? String.valueOf(totalAvgGasPrice / count) : "0");

            // 近7天Gas趋势
            List<Map<String, Object>> gasTrend = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.format(DATE_FMT);
                BigDecimal dayFee = BigDecimal.ZERO;
                for (BlockchainGasFee fee : allFees) {
                    if (date.equals(fee.getStatDate()) && fee.getTotalFeeEth() != null) {
                        dayFee = dayFee.add(fee.getTotalFeeEth());
                    }
                }
                Map<String, Object> item = new HashMap<>();
                item.put("date", dateStr);
                item.put("feeEth", dayFee.toPlainString());
                gasTrend.add(item);
            }
            gas.put("gasTrend", gasTrend);
        } catch (Exception e) {
            log.warn("构建Gas统计失败: {}", e.getMessage());
            gas.put("totalFeeEth", "0");
            gas.put("todayFeeEth", "0");
            gas.put("avgGasPrice", "0");
            gas.put("gasTrend", Collections.emptyList());
        }
        return gas;
    }
}
