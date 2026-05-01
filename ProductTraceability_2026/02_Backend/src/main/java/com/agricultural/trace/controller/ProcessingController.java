package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.TraceProcessing;
import com.agricultural.trace.service.AdminBusinessNotificationService;
import com.agricultural.trace.service.ProcessingService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 加工记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/processing")
@RequiredArgsConstructor
@CrossOrigin
public class ProcessingController {

    private final ProcessingService processingService;
    private final AdminBusinessNotificationService adminBusinessNotificationService;

    /**
     * 创建加工记录
     * Frontend sends enterpriseId, mapped to processingEnterpriseId
     */
    @OperationLog("新增加工记录")
    @PostMapping("/create")
    public Result<TraceProcessing> create(@RequestBody TraceProcessing record) {
        log.info("新增加工记录, batchId={}, sourceBatchCode={}", record.getBatchId(), record.getSourceBatchCode());
        try {
            // Map frontend enterpriseId -> processingEnterpriseId
            if (record.getProcessingEnterpriseId() == null && record.getEnterpriseId() != null) {
                record.setProcessingEnterpriseId(record.getEnterpriseId());
            }

            TraceProcessing created = processingService.createProcessing(record);
            adminBusinessNotificationService.notifyProcessingCreated(created);
            return Result.success("加工记录创建成功", created);
        } catch (Exception e) {
            log.error("新增加工记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询加工记录列表 (支持 enterpriseId + keyword 筛选)
     */
    @GetMapping("/list")
    public Result<Page<java.util.Map<String, Object>>> getList(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String keyword
    ) {
        log.info("query processing list, pageNum={}, pageSize={}, batchId={}, enterpriseId={}, keyword={}",
                pageNum, pageSize, batchId, enterpriseId, keyword);
        Page<java.util.Map<String, Object>> page = processingService.getProcessingList(
                pageNum, pageSize, batchId, enterpriseId, keyword);
        return Result.success(page);
    }

    /**
     * 更新加工记录
     */
    @OperationLog("更新加工记录")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody TraceProcessing record) {
        log.info("更新加工记录, id={}", record.getId());
        try {
            if (record.getId() == null) {
                return Result.error("记录ID不能为空");
            }
            if (record.getProcessingEnterpriseId() == null && record.getEnterpriseId() != null) {
                record.setProcessingEnterpriseId(record.getEnterpriseId());
            }
            processingService.updateProcessing(record);
            return Result.success("更新成功", null);
        } catch (Exception e) {
            log.error("更新加工记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除加工记录
     */
    @OperationLog("删除加工记录")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除加工记录, id={}", id);
        try {
            processingService.deleteProcessing(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除加工记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
