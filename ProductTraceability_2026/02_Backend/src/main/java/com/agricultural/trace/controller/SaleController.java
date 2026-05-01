package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.TraceSale;
import com.agricultural.trace.service.SaleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 销售记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/sale")
@RequiredArgsConstructor
@CrossOrigin
public class SaleController {

    private final SaleService saleService;

    /**
     * 查询批次可销售数量
     * GET /api/sale/available-quantity
     */
    @GetMapping("/available-quantity")
    public Result<java.util.Map<String, Object>> getAvailableSaleQuantity(
            @RequestParam Long batchId,
            @RequestParam(required = false) Long enterpriseId
    ) {
        try {
            java.util.Map<String, Object> data = saleService.getAvailableSaleQuantity(batchId, enterpriseId);
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询可销售数量失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建销售记录
     * Frontend sends enterpriseId, mapped to saleEnterpriseId
     */
    @OperationLog("创建销售记录")
    @PostMapping("/create")
    public Result<TraceSale> createSale(@RequestBody TraceSale record) {
        log.info("创建销售记录, batchId={}, batchCode={}", record.getBatchId(), record.getBatchCode());
        try {
            // Map frontend enterpriseId -> saleEnterpriseId
            if (record.getSaleEnterpriseId() == null && record.getEnterpriseId() != null) {
                record.setSaleEnterpriseId(record.getEnterpriseId());
            }

            TraceSale result = saleService.createSale(record);
            return Result.success("销售记录创建成功", result);
        } catch (Exception e) {
            log.error("创建销售记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新销售记录
     */
    @OperationLog("更新销售记录")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody TraceSale record) {
        log.info("更新销售记录, id={}", record.getId());
        try {
            if (record.getId() == null) {
                return Result.error("记录ID不能为空");
            }
            if (record.getSaleEnterpriseId() == null && record.getEnterpriseId() != null) {
                record.setSaleEnterpriseId(record.getEnterpriseId());
            }
            saleService.updateSale(record);
            return Result.success("更新成功", null);
        } catch (Exception e) {
            log.error("更新销售记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询销售记录列表
     *
     * @param pageNum 页码（默认1）
     * @param pageSize 每页大小（默认10）
     * @param batchId 批次ID（可选）
     * @param enterpriseId 企业ID（可选）
     * @param keyword 关键词（可选）
     * @return 销售记录分页列表
     */
    @GetMapping("/list")
    public Result<Page<TraceSale>> getSaleList(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String keyword
    ) {
        log.info("查询销售记录列表, pageNum={}, pageSize={}, batchId={}, enterpriseId={}, keyword={}", 
                pageNum, pageSize, batchId, enterpriseId, keyword);
        try {
            Page<TraceSale> page = saleService.getSaleList(pageNum, pageSize, batchId, enterpriseId, keyword);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询销售记录列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
