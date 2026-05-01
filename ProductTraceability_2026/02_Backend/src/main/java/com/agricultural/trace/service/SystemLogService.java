package com.agricultural.trace.service;

import com.agricultural.trace.entity.SysLoginLog;
import com.agricultural.trace.entity.SysOperationLog;
import com.agricultural.trace.mapper.SysLoginLogMapper;
import com.agricultural.trace.mapper.SysOperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogService {

    private final SysOperationLogMapper operationLogMapper;
    private final SysLoginLogMapper loginLogMapper;

    public Page<SysOperationLog> getOperationLogs(Integer current, Integer size,
                                                   String username, String operation,
                                                   Integer status, String startDate, String endDate) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(SysOperationLog::getUsername, username);
        }
        if (StringUtils.hasText(operation)) {
            wrapper.like(SysOperationLog::getOperation, operation);
        }
        if (status != null) {
            wrapper.eq(SysOperationLog::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(SysOperationLog::getCreateTime, LocalDateTime.of(LocalDate.parse(startDate), LocalTime.MIN));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(SysOperationLog::getCreateTime, LocalDateTime.of(LocalDate.parse(endDate), LocalTime.MAX));
        }
        wrapper.orderByDesc(SysOperationLog::getCreateTime);
        return operationLogMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public Page<SysLoginLog> getLoginLogs(Integer current, Integer size,
                                           String username, Integer status,
                                           String startDate, String endDate) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(SysLoginLog::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(SysLoginLog::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(SysLoginLog::getLoginTime, LocalDateTime.of(LocalDate.parse(startDate), LocalTime.MIN));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(SysLoginLog::getLoginTime, LocalDateTime.of(LocalDate.parse(endDate), LocalTime.MAX));
        }
        wrapper.orderByDesc(SysLoginLog::getLoginTime);
        return loginLogMapper.selectPage(new Page<>(current, size), wrapper);
    }
}
