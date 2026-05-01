package com.agricultural.trace.controller;

import com.agricultural.trace.common.Result;
import com.agricultural.trace.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 首页看板控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 养殖企业首页统计
     */
    @GetMapping("/farmer-stats")
    public Result<Map<String, Object>> getFarmerStats(
            @RequestParam(required = false) Long enterpriseId
    ) {
        log.info("查询养殖企业首页统计, enterpriseId={}", enterpriseId);
        try {
            Map<String, Object> stats = dashboardService.getFarmerStats(enterpriseId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("查询养殖企业统计失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 加工宰杀企业首页统计
     */
    @GetMapping("/processor-stats")
    public Result<Map<String, Object>> getProcessorStats(
            @RequestParam(required = false) Long enterpriseId
    ) {
        log.info("查询加工企业首页统计, enterpriseId={}", enterpriseId);
        try {
            Map<String, Object> stats = dashboardService.getProcessorStats(enterpriseId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("查询加工企业统计失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检疫质检企业首页统计
     */
    @GetMapping("/quarantine-stats")
    public Result<Map<String, Object>> getQuarantineStats(
            @RequestParam(required = false) Long enterpriseId
    ) {
        log.info("查询检疫企业首页统计, enterpriseId={}", enterpriseId);
        try {
            Map<String, Object> stats = dashboardService.getQuarantineStats(enterpriseId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("查询检疫企业统计失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理员端统计大屏
     */
    @GetMapping("/admin-stats")
    public Result<Map<String, Object>> getAdminStats(
            @RequestParam(required = false) String role
    ) {
        log.info("查询管理员端统计大屏, role={}", role);
        try {
            Map<String, Object> stats = dashboardService.getAdminStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("查询管理员统计失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 数据统计分析页
     */
    @GetMapping("/analysis-stats")
    public Result<Map<String, Object>> getAnalysisStats(
            @RequestParam(required = false) Long enterpriseId
    ) {
        log.info("查询数据统计分析, enterpriseId={}", enterpriseId);
        try {
            Map<String, Object> stats = dashboardService.getAnalysisStats(enterpriseId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("查询数据统计分析失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
