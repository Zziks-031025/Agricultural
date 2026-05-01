package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.SysConfig;
import com.agricultural.trace.service.SystemConfigService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
@CrossOrigin
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/list")
    public Result<Page<SysConfig>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String configType,
            @RequestParam(required = false) String keyword) {
        try {
            return Result.success(systemConfigService.getConfigList(current, size, configType, keyword));
        } catch (Exception e) {
            log.error("查询系统配置失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog("新增系统配置")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody SysConfig config) {
        try {
            systemConfigService.addConfig(config);
            return Result.success();
        } catch (Exception e) {
            log.error("新增系统配置失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog("更新系统配置")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysConfig config) {
        try {
            systemConfigService.updateConfig(config);
            return Result.success();
        } catch (Exception e) {
            log.error("更新系统配置失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog("删除系统配置")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            systemConfigService.deleteConfig(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除系统配置失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据配置键获取配置值
     */
    @GetMapping("/value/{configKey}")
    public Result<String> getConfigValue(@PathVariable String configKey) {
        try {
            String value = systemConfigService.getConfigValue(configKey);
            if (value == null) {
                return Result.error("配置不存在");
            }
            return Result.success(value);
        } catch (Exception e) {
            log.error("获取配置值失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
