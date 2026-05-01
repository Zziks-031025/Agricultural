package com.agricultural.trace.controller;

import com.agricultural.trace.common.Result;
import com.agricultural.trace.service.ScanStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 溯源查询统计控制器（管理员端）
 * 统计用户扫码查询的次数和分布
 */
@Slf4j
@RestController
@RequestMapping("/api/scan-stats")
@RequiredArgsConstructor
@CrossOrigin
public class ScanStatsController {

    private final ScanStatsService scanStatsService;

    /**
     * 获取溯源查询统计概览
     * 返回: 总查询次数、今日查询、近7天趋势、热门产品、设备分布等
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        try {
            return Result.success(scanStatsService.getOverview());
        } catch (Exception e) {
            log.error("查询溯源统计概览失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取查询趋势（近30天每日查询量）
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend(
            @RequestParam(defaultValue = "30") int days) {
        try {
            return Result.success(scanStatsService.getDailyTrend(days));
        } catch (Exception e) {
            log.error("查询趋势统计失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取查询日志列表（分页）
     */
    @GetMapping("/logs")
    public Result<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String batchCode,
            @RequestParam(required = false) String deviceType) {
        try {
            return Result.success(scanStatsService.getLogs(current, size, batchCode, deviceType));
        } catch (Exception e) {
            log.error("查询扫码日志失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
