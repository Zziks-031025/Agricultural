package com.agricultural.trace.controller;

import com.agricultural.trace.common.Result;
import com.agricultural.trace.service.TraceService;
import com.agricultural.trace.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 溯源详情控制器（消费者端）
 * 提供扫码溯源数据聚合查询和区块链验真能力
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin
public class TraceController {

    private final TraceService traceService;
    private final JwtUtils jwtUtils;

    /**
     * 获取溯源详情
     * 聚合 批次信息 + 全生命周期时间轴 + 企业信息
     *
     * 请求示例：
     * GET /api/trace/detail?batchId=1001
     * GET /api/trace/detail?batchId=BATCH202602010001
     *
     * 返回结构：
     * {
     *   "batchInfo": { productName, batchCode, productionDate, ... },
     *   "processes": [
     *     { nodeType:"breeding_init", processName, operationTime, txHash, ... },
     *     { nodeType:"quarantine", inspectionResult, inspector, certificateUrl, official:true, ... },
     *     { nodeType:"processing", ... },
     *     ...
     *   ],
     *   "enterpriseInfo": { name, logo, contactPhone, enterpriseCode, licenseNo }
     * }
     *
     * @param batchId 批次ID 或 批次编号
     * @return 聚合后的溯源详情
     */
    @GetMapping("/trace/detail")
    public Result<Map<String, Object>> getTraceDetail(@RequestParam String batchId,
                                                      @RequestHeader(value = "Authorization", required = false) String token,
                                                      HttpServletRequest request) {
        log.info("查询溯源详情，batchId: {}", batchId);

        try {
            Map<String, Object> detail = traceService.getTraceDetail(
                    batchId,
                    extractUserIdentity(token),
                    resolveClientIp(request),
                    request != null ? request.getHeader("User-Agent") : null
            );
            return Result.success(detail);
        } catch (RuntimeException e) {
            log.error("查询溯源详情失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 区块链验真
     * 一次性验证该批次所有环节（养殖、检疫、加工等）的数据完整性
     *
     * 请求示例：
     * POST /api/blockchain/verify  { "batchId": "1001" }
     *
     * 返回结构：
     * {
     *   "verified": true,
     *   "totalNodes": 3,
     *   "verifiedNodes": 3,
     *   "blockNumber": 18956234,
     *   "nodeResults": [
     *     { "nodeName":"养殖批次", "verified":true, "txHash":"0x..." },
     *     { "nodeName":"检疫质检", "verified":true, "txHash":"0x...", "official":true }
     *   ]
     * }
     *
     * @param body 包含 batchId 的请求体
     * @return 验证结果
     */
    @PostMapping("/blockchain/verify")
    public Result<Map<String, Object>> verifyBlockchain(@RequestBody Map<String, String> body,
                                                        @RequestHeader(value = "Authorization", required = false) String token,
                                                        HttpServletRequest request) {
        String batchId = body.get("batchId");
        if (batchId == null || batchId.isEmpty()) {
            batchId = body.get("batchCode");
        }
        log.info("区块链验真，batchId: {}", batchId);

        try {
            Map<String, Object> result = traceService.verifyBlockchain(
                    batchId,
                    extractUserIdentity(token),
                    resolveClientIp(request),
                    request != null ? request.getHeader("User-Agent") : null,
                    null
            );
            return Result.success(result);
        } catch (RuntimeException e) {
            log.error("区块链验真失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    private String extractUserIdentity(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            String rawToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Long userId = jwtUtils.getUserIdFromToken(rawToken);
            String username = jwtUtils.getUsernameFromToken(rawToken);
            if (username != null && !username.isEmpty()) {
                return username;
            }
            return userId != null ? String.valueOf(userId) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
