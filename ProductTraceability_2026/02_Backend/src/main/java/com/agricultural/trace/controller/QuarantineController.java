package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.dto.QuarantineSubmitDTO;
import com.agricultural.trace.service.AdminBusinessNotificationService;
import com.agricultural.trace.service.QuarantineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 检疫质检控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/quarantine")
@RequiredArgsConstructor
@CrossOrigin
public class QuarantineController {

    private final QuarantineService quarantineService;
    private final AdminBusinessNotificationService adminBusinessNotificationService;

    @OperationLog("提交检疫申请")
    @PostMapping("/apply")
    public Result<Map<String, Object>> applyQuarantine(@RequestBody Map<String, Object> params) {
        log.info("receive quarantine apply request, params={}", params);
        try {
            Map<String, Object> result = quarantineService.applyQuarantine(params);
            adminBusinessNotificationService.notifyQuarantineApplied(params, result);
            return Result.success("检疫申请已提交，请等待检疫机构受理", result);
        } catch (RuntimeException e) {
            log.error("quarantine apply failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog("删除检疫申请")
    @DeleteMapping("/apply/delete/{id}")
    public Result<Void> deleteApply(@PathVariable Long id) {
        log.info("delete quarantine apply, id={}", id);
        try {
            quarantineService.deleteApply(id);
            return Result.success("删除成功", null);
        } catch (RuntimeException e) {
            log.error("delete quarantine apply failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/apply/list")
    public Result<List<Map<String, Object>>> listApply(
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String batchCode,
            @RequestParam(required = false) String status) {
        log.info("query quarantine apply list, enterpriseId={}, batchCode={}, status={}", enterpriseId, batchCode, status);
        try {
            return Result.success(quarantineService.listApplyByEnterprise(enterpriseId, batchCode, status));
        } catch (RuntimeException e) {
            log.error("query quarantine apply list failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/apply/list-for-inspector")
    public Result<List<Map<String, Object>>> listApplyForInspector(
            @RequestParam(required = false) Long inspectionEnterpriseId,
            @RequestParam(required = false) String batchCode,
            @RequestParam(required = false) String status) {
        log.info("query quarantine apply list for inspector, inspectionEnterpriseId={}, batchCode={}, status={}",
                inspectionEnterpriseId, batchCode, status);
        try {
            List<Map<String, Object>> list = quarantineService.listApplyForInspector(inspectionEnterpriseId, batchCode, status);
            log.info("query quarantine apply list result size: {}", list.size());
            return Result.success(list);
        } catch (RuntimeException e) {
            log.error("query quarantine apply list for inspector failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog("提交检疫结果")
    @PostMapping("/submit")
    public Result<Map<String, Object>> submitQuarantine(@Validated @RequestBody QuarantineSubmitDTO dto) {
        log.info("接收检疫结果提交请求, batchId: {}, checkResult: {}, inspectorId: {}",
                dto.getBatchId(), dto.getCheckResult(), dto.getInspectorId());

        try {
            Map<String, Object> result = quarantineService.submitQuarantine(dto);
            adminBusinessNotificationService.notifyQuarantineSubmitted(dto, result);

            String txHash = result.get("transactionHash") != null ? result.get("transactionHash").toString() : null;
            if (txHash != null) {
                log.info("检疫结果提交成功并已上链, batchId: {}, txHash: {}", dto.getBatchId(), txHash);
                return Result.success("检疫结果提交成功，已上链存证", result);
            }
            log.warn("检疫结果已保存但上链失败, batchId: {}", dto.getBatchId());
            return Result.success("检疫结果已保存，上链待重试", result);
        } catch (RuntimeException e) {
            log.error("检疫结果提交失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @OperationLog("更新检疫证书")
    @PostMapping("/update-certificate")
    public Result<Map<String, Object>> updateCertificate(@RequestBody Map<String, Object> params) {
        log.info("接收检疫证书更新请求, params: {}", params);

        try {
            Long id = params.get("id") != null ? Long.parseLong(params.get("id").toString()) : null;
            if (id == null) {
                return Result.error("检疫记录ID不能为空");
            }

            String certImage = params.get("certImage") != null ? params.get("certImage").toString() : null;
            String certNo = params.get("certNo") != null ? params.get("certNo").toString() : null;
            String remark = params.get("remark") != null ? params.get("remark").toString() : null;

            Map<String, Object> result = quarantineService.updateCertificate(id, certImage, certNo, remark);
            return Result.success("证书信息更新成功", result);
        } catch (RuntimeException e) {
            log.error("检疫证书更新失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }
}
