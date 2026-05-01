package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.TraceField;
import com.agricultural.trace.entity.TraceStage;
import com.agricultural.trace.entity.TraceTemplate;
import com.agricultural.trace.service.TemplateService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/template")
@RequiredArgsConstructor
@CrossOrigin
public class TemplateController {

    private final TemplateService templateService;

    // ==================== 模版管理 ====================

    @GetMapping("/list")
    public Result<Page<TraceTemplate>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "999") Integer size,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) Integer templateType) {
        try {
            return Result.success(templateService.getTemplateList(current, size, templateName, templateType));
        } catch (Exception e) {
            log.error("查询模版列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/detail/{id}")
    public Result<TraceTemplate> detail(@PathVariable Long id) {
        try {
            return Result.success(templateService.getTemplateDetail(id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/system-field-catalog")
    public Result<List<Map<String, Object>>> systemFieldCatalog() {
        try {
            return Result.success(templateService.getSystemFieldCatalog());
        } catch (Exception e) {
            log.error("鏌ヨ绯荤粺瀛楁鐩綍澶辫触: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "模板管理", operation = "新增模板")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody TraceTemplate template) {
        try {
            templateService.addTemplate(template);
            return Result.success();
        } catch (Exception e) {
            log.error("新增模版失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "模板管理", operation = "更新模板")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody TraceTemplate template) {
        try {
            templateService.updateTemplate(template);
            return Result.success();
        } catch (Exception e) {
            log.error("更新模版失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "模板管理", operation = "删除模板")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            templateService.deleteTemplate(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除模版失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "模板管理", operation = "切换模板状态")
    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            templateService.toggleTemplateStatus(id, status);
            return Result.success();
        } catch (Exception e) {
            log.error("切换模版状态失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    // ==================== 环节管理 ====================

    @GetMapping("/stage/list/{templateId}")
    public Result<List<TraceStage>> stageList(@PathVariable Long templateId) {
        try {
            return Result.success(templateService.getStageList(templateId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "模板管理", operation = "新增环节")
    @PostMapping("/stage/add")
    public Result<Void> addStage(@RequestBody TraceStage stage) {
        try {
            templateService.addStage(stage);
            return Result.success();
        } catch (Exception e) {
            log.error("新增环节失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "模板管理", operation = "更新环节")
    @PutMapping("/stage/update")
    public Result<Void> updateStage(@RequestBody TraceStage stage) {
        try {
            templateService.updateStage(stage);
            return Result.success();
        } catch (Exception e) {
            log.error("更新环节失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "模板管理", operation = "删除环节")
    @DeleteMapping("/stage/delete/{id}")
    public Result<Void> deleteStage(@PathVariable Long id) {
        try {
            templateService.deleteStage(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除环节失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    // ==================== 字段管理 ====================

    @GetMapping("/field/list/{stageId}")
    public Result<List<TraceField>> fieldList(@PathVariable Long stageId) {
        try {
            return Result.success(templateService.getFieldList(stageId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "模板管理", operation = "新增字段")
    @PostMapping("/field/add")
    public Result<Void> addField(@RequestBody TraceField field) {
        try {
            templateService.addField(field);
            return Result.success();
        } catch (Exception e) {
            log.error("新增字段失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "模板管理", operation = "更新字段")
    @PutMapping("/field/update")
    public Result<Void> updateField(@RequestBody TraceField field) {
        try {
            templateService.updateField(field);
            return Result.success();
        } catch (Exception e) {
            log.error("更新字段失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog(module = "模板管理", operation = "删除字段")
    @DeleteMapping("/field/delete/{id}")
    public Result<Void> deleteField(@PathVariable Long id) {
        try {
            templateService.deleteField(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除字段失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
