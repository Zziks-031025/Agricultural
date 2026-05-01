package com.agricultural.trace.controller;

import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.SysLoginLog;
import com.agricultural.trace.entity.SysOperationLog;
import com.agricultural.trace.service.SystemLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/system/log")
@RequiredArgsConstructor
@CrossOrigin
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping("/operation")
    public Result<Page<SysOperationLog>> operationLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Page<SysOperationLog> page = systemLogService.getOperationLogs(current, size, username, operation, status, startDate, endDate);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询操作日志失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/login")
    public Result<Page<SysLoginLog>> loginLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Page<SysLoginLog> page = systemLogService.getLoginLogs(current, size, username, status, startDate, endDate);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询登录日志失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
