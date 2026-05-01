package com.agricultural.trace.controller;

import com.agricultural.trace.common.Result;
import com.agricultural.trace.service.BlockchainNodeService;
import com.agricultural.trace.service.BlockchainStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 区块链数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
@CrossOrigin
public class BlockchainController {

    private final BlockchainStatsService blockchainStatsService;
    private final BlockchainNodeService blockchainNodeService;

    /**
     * 获取区块链统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(
            @RequestParam(required = false) Long enterpriseId) {
        log.info("查询区块链统计数据, enterpriseId={}", enterpriseId);
        try {
            Map<String, Object> stats = blockchainStatsService.getStats(enterpriseId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("查询区块链统计失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 区块链上链日志列表(企业端)
     */
    @GetMapping("/logs")
    public Result<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String txHash,
            @RequestParam(required = false) String batchCode) {
        try {
            Map<String, Object> result = blockchainStatsService.getLogs(
                    pageNum, pageSize, enterpriseId, bizType, status, txHash, batchCode);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询上链日志失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 失败记录重新上链
     */
    @PostMapping("/retry/{id}")
    public Result<Void> retry(@PathVariable Long id) {
        try {
            blockchainStatsService.retryChain(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("重新上链失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取区块链节点状态
     */
    @GetMapping("/node/status")
    public Result<Map<String, Object>> getNodeStatus() {
        try {
            Map<String, Object> status = blockchainNodeService.getNodeStatus();
            return Result.success(status);
        } catch (Exception e) {
            log.error("查询节点状态失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
