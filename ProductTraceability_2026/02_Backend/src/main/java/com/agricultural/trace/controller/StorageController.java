package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.TraceStorage;
import com.agricultural.trace.service.AdminBusinessNotificationService;
import com.agricultural.trace.service.StorageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仓储管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@CrossOrigin
public class StorageController {

    private final StorageService storageService;
    private final AdminBusinessNotificationService adminBusinessNotificationService;

    @GetMapping("/available-quantity")
    public Result<java.util.Map<String, Object>> getAvailableQuantity(
            @RequestParam Long batchId,
            @RequestParam(required = false) Long enterpriseId
    ) {
        log.info("查询仓储可操作数量, batchId={}, enterpriseId={}", batchId, enterpriseId);
        try {
            return Result.success(storageService.getAvailableQuantity(batchId, enterpriseId));
        } catch (Exception e) {
            log.error("查询仓储可操作数量失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<Page<java.util.Map<String, Object>>> getList(
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) Integer storageType,
            @RequestParam(required = false) String keyword
    ) {
        Long actualCurrent = current != null ? current : (pageNum != null ? pageNum : 1L);
        Long actualSize = size != null ? size : (pageSize != null ? pageSize : 10L);
        log.info("查询仓储记录, current={}, size={}, batchId={}, enterpriseId={}, storageType={}, keyword={}",
                actualCurrent, actualSize, batchId, enterpriseId, storageType, keyword);
        return Result.success(storageService.getStorageList(actualCurrent, actualSize, batchId, enterpriseId, storageType, keyword));
    }

    @OperationLog("新增仓储记录")
    @PostMapping("/create")
    public Result<TraceStorage> create(@RequestBody TraceStorage record) {
        log.info("新增仓储记录, batchId={}, batchCode={}", record.getBatchId(), record.getBatchCode());
        try {
            if (record.getStorageEnterpriseId() == null && record.getEnterpriseId() != null) {
                record.setStorageEnterpriseId(record.getEnterpriseId());
            }
            TraceStorage created = storageService.createStorage(record);
            adminBusinessNotificationService.notifyStorageCreated(created);
            return Result.success("入库记录创建成功", created);
        } catch (Exception e) {
            log.error("新增仓储记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog("更新仓储记录")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody TraceStorage record) {
        log.info("更新仓储记录, id={}", record.getId());
        try {
            if (record.getId() == null) {
                return Result.error("记录ID不能为空");
            }
            if (record.getStorageEnterpriseId() == null && record.getEnterpriseId() != null) {
                record.setStorageEnterpriseId(record.getEnterpriseId());
            }
            storageService.updateStorage(record);
            return Result.success("更新成功", null);
        } catch (Exception e) {
            log.error("更新仓储记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog("删除仓储记录")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除仓储记录, id={}", id);
        try {
            storageService.deleteStorage(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除仓储记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
