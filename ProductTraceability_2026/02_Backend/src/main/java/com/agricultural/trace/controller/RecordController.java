package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.TraceRecord;
import com.agricultural.trace.service.AdminBusinessNotificationService;
import com.agricultural.trace.service.RecordService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 生长记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/record")
@RequiredArgsConstructor
@CrossOrigin
public class RecordController {

    private final RecordService recordService;
    private final AdminBusinessNotificationService adminBusinessNotificationService;

    @GetMapping("/list")
    public Result<Page<TraceRecord>> getRecordList(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String recordType
    ) {
        log.info("查询生长记录, pageNum={}, pageSize={}, batchId={}, recordType={}",
                pageNum, pageSize, batchId, recordType);
        return Result.success(recordService.getRecordList(pageNum, pageSize, batchId, recordType));
    }

    @OperationLog("新增生长记录")
    @PostMapping("/create")
    public Result<TraceRecord> createRecord(@RequestBody TraceRecord record) {
        log.info("新增生长记录, batchCode={}, batchId={}, type={}", record.getBatchCode(), record.getBatchId(), record.getRecordType());
        try {
            TraceRecord created = recordService.createRecordWithBatchCode(record);
            adminBusinessNotificationService.notifyRecordCreated(created);
            return Result.success("记录添加成功", created);
        } catch (Exception e) {
            log.error("新增记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog("删除生长记录")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        log.info("删除生长记录, id={}", id);
        try {
            recordService.deleteRecord(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
