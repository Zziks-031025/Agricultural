package com.agricultural.trace.controller;

import com.agricultural.trace.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@CrossOrigin
public class DebugController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/check-data")
    public Result<Map<String, Object>> checkData() {
        Map<String, Object> result = new HashMap<>();

        // 1. 企业统计
        String sql1 = "SELECT enterprise_type, COUNT(*) as count FROM enterprise_info WHERE status = 1 GROUP BY enterprise_type";
        List<Map<String, Object>> enterprises = jdbcTemplate.queryForList(sql1);
        result.put("enterprises", enterprises);

        // 2. 批次总数
        Long totalBatches = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trace_batch", Long.class);
        result.put("totalBatches", totalBatches);

        // 3. 批次状态分布
        String sql3 = "SELECT batch_status, COUNT(*) as count FROM trace_batch GROUP BY batch_status";
        List<Map<String, Object>> batchStatus = jdbcTemplate.queryForList(sql3);
        result.put("batchStatus", batchStatus);

        // 4. 上链记录数
        Long onChainCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trace_batch WHERE tx_hash IS NOT NULL AND tx_hash != ''", Long.class);
        result.put("onChainCount", onChainCount);

        // 5. 用户总数
        Long totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user WHERE status = 1", Long.class);
        result.put("totalUsers", totalUsers);

        // 6. 检疫记录总数
        Long totalInspections = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trace_inspection", Long.class);
        result.put("totalInspections", totalInspections);

        // 7. 批次详细列表
        String sql7 = "SELECT id, batch_code, product_name, batch_status, tx_hash, create_time FROM trace_batch ORDER BY create_time DESC";
        List<Map<String, Object>> batches = jdbcTemplate.queryForList(sql7);
        result.put("batches", batches);

        return Result.success(result);
    }
}
