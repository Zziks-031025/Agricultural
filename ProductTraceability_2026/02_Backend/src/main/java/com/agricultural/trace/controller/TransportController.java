package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.TraceTransport;
import com.agricultural.trace.service.AdminBusinessNotificationService;
import com.agricultural.trace.service.TransportService;
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
 * 运输管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/transport")
@RequiredArgsConstructor
@CrossOrigin
public class TransportController {

    private final TransportService transportService;
    private final AdminBusinessNotificationService adminBusinessNotificationService;

    @OperationLog(module = "运输管理", operation = "创建运输记录")
    @PostMapping("/create")
    public Result<TraceTransport> create(@RequestBody TraceTransport record) {
        log.info("新增运输记录, batchId={}, batchCode={}", record.getBatchId(), record.getBatchCode());
        try {
            if (record.getTransportEnterpriseId() == null && record.getEnterpriseId() != null) {
                record.setTransportEnterpriseId(record.getEnterpriseId());
            }
            TraceTransport created = transportService.createTransport(record);
            adminBusinessNotificationService.notifyTransportCreated(created);
            return Result.success("运输记录创建成功", created);
        } catch (Exception e) {
            log.error("新增运输记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "运输管理", operation = "更新运输记录")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody TraceTransport record) {
        log.info("更新运输记录, id={}", record.getId());
        try {
            if (record.getId() == null) {
                return Result.error("记录ID不能为空");
            }
            if (record.getTransportEnterpriseId() == null && record.getEnterpriseId() != null) {
                record.setTransportEnterpriseId(record.getEnterpriseId());
            }
            transportService.updateTransport(record);
            return Result.success("更新成功", null);
        } catch (Exception e) {
            log.error("更新运输记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "运输管理", operation = "删除运输记录")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除运输记录, id={}", id);
        try {
            transportService.deleteTransport(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除运输记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/available-quantity")
    public Result<java.util.Map<String, Object>> getAvailableTransportQuantity(
            @RequestParam Long batchId,
            @RequestParam(required = false) Long enterpriseId
    ) {
        log.info("查询可运输数量, batchId={}, enterpriseId={}", batchId, enterpriseId);
        try {
            return Result.success(transportService.getAvailableTransportQuantity(batchId, enterpriseId));
        } catch (Exception e) {
            log.error("查询可运输数量失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<Page<TraceTransport>> getList(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) Long receiveEnterpriseId,
            @RequestParam(required = false) String keyword
    ) {
        log.info("查询运输记录, pageNum={}, pageSize={}, batchId={}, enterpriseId={}, receiveEnterpriseId={}, keyword={}",
                pageNum, pageSize, batchId, enterpriseId, receiveEnterpriseId, keyword);
        return Result.success(transportService.getTransportList(pageNum, pageSize, batchId, enterpriseId, receiveEnterpriseId, keyword));
    }
}
