package com.agricultural.trace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 溯源查询统计服务
 * 基于 user_scan_log 表提供扫码查询的统计分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanStatsService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 统计概览
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 总查询次数
        Long totalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_scan_log", Long.class);
        result.put("totalCount", totalCount != null ? totalCount : 0);

        // 今日查询次数
        Long todayCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_scan_log WHERE DATE(scan_time) = CURDATE()", Long.class);
        result.put("todayCount", todayCount != null ? todayCount : 0);

        // 昨日查询次数（用于计算环比）
        Long yesterdayCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_scan_log WHERE DATE(scan_time) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)",
                Long.class);
        result.put("yesterdayCount", yesterdayCount != null ? yesterdayCount : 0);

        // 本周查询次数
        Long weekCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_scan_log WHERE YEARWEEK(scan_time, 1) = YEARWEEK(CURDATE(), 1)",
                Long.class);
        result.put("weekCount", weekCount != null ? weekCount : 0);

        // 被查询批次数（去重）
        Long batchCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT batch_code) FROM user_scan_log", Long.class);
        result.put("queriedBatchCount", batchCount != null ? batchCount : 0);

        // 设备类型分布
        List<Map<String, Object>> deviceDist = jdbcTemplate.queryForList(
                "SELECT IFNULL(device_type, 'unknown') AS name, COUNT(*) AS value " +
                        "FROM user_scan_log GROUP BY device_type ORDER BY value DESC");
        result.put("deviceDistribution", deviceDist);

        // 热门查询产品 TOP10
        List<Map<String, Object>> hotProducts = jdbcTemplate.queryForList(
                "SELECT l.batch_code AS batchCode, " +
                        "IFNULL(b.product_name, l.batch_code) AS productName, " +
                        "COUNT(*) AS queryCount " +
                        "FROM user_scan_log l " +
                        "LEFT JOIN trace_batch b ON (l.batch_id = b.id OR l.batch_code = b.batch_code) " +
                        "GROUP BY l.batch_code, b.product_name " +
                        "ORDER BY queryCount DESC LIMIT 10");
        result.put("hotProducts", hotProducts);

        // 近7天趋势（简要版）
        List<Map<String, Object>> weekTrend = jdbcTemplate.queryForList(
                "SELECT DATE(scan_time) AS date, COUNT(*) AS count " +
                        "FROM user_scan_log " +
                        "WHERE scan_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
                        "GROUP BY DATE(scan_time) ORDER BY date");
        result.put("weekTrend", weekTrend);

        return result;
    }

    /**
     * 每日查询趋势
     */
    public Map<String, Object> getDailyTrend(int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> trend = jdbcTemplate.queryForList(
                "SELECT DATE(scan_time) AS date, COUNT(*) AS count " +
                        "FROM user_scan_log " +
                        "WHERE scan_time >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                        "GROUP BY DATE(scan_time) ORDER BY date",
                days - 1);
        result.put("trend", trend);
        result.put("days", days);
        return result;
    }

    /**
     * 查询日志列表（分页）
     */
    public Map<String, Object> getLogs(int current, int size,
                                       String batchCode, String deviceType) {
        Map<String, Object> result = new LinkedHashMap<>();
        int offset = (current - 1) * size;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (batchCode != null && !batchCode.isEmpty()) {
            where.append(" AND l.batch_code LIKE ? ");
            params.add("%" + batchCode + "%");
        }
        if (deviceType != null && !deviceType.isEmpty()) {
            where.append(" AND l.device_type = ? ");
            params.add(deviceType);
        }

        // total
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_scan_log l" + where,
                Long.class, params.toArray());
        result.put("total", total != null ? total : 0);

        // records
        params.add(size);
        params.add(offset);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT l.id, l.batch_code AS batchCode, " +
                        "IFNULL(b.product_name, l.batch_code) AS productName, " +
                        "l.openid, l.scan_time AS scanTime, " +
                        "l.scan_location AS scanLocation, " +
                        "l.ip_address AS ipAddress, " +
                        "l.device_type AS deviceType " +
                        "FROM user_scan_log l " +
                        "LEFT JOIN trace_batch b ON (l.batch_id = b.id " +
                        "OR (l.batch_id IS NULL AND l.batch_code = b.batch_code)) " +
                        where +
                        " ORDER BY l.scan_time DESC LIMIT ? OFFSET ?",
                params.toArray());
        result.put("records", records);
        result.put("current", current);
        result.put("size", size);
        return result;
    }
}
