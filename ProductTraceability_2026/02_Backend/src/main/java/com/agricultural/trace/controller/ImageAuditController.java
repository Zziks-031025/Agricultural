package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.entity.EnterpriseAuditImage;
import com.agricultural.trace.service.ImageAuditService;
import com.agricultural.trace.utils.JwtUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业图片审核控制器
 * 管理端：查看审核列表、通过/拒绝审核
 * 企业端：提交图片变更审核、查看本企业审核记录
 */
@Slf4j
@RestController
@RequestMapping("/api/image-audit")
@RequiredArgsConstructor
@CrossOrigin
public class ImageAuditController {

    private final ImageAuditService imageAuditService;
    private final JwtUtils jwtUtils;

    /**
     * 企业提交图片变更审核
     */
    @OperationLog(module = "图片审核", operation = "提交图片变更审核")
    @PostMapping("/submit")
    public Result<EnterpriseAuditImage> submit(@RequestBody Map<String, Object> body) {
        try {
            Long enterpriseId = body.get("enterpriseId") != null ? Long.parseLong(body.get("enterpriseId").toString()) : null;
            Long userId = body.get("userId") != null ? Long.parseLong(body.get("userId").toString()) : null;
            String fieldName = (String) body.get("fieldName");
            String oldValue = (String) body.get("oldValue");
            String newValue = (String) body.get("newValue");

            if (enterpriseId == null || fieldName == null || newValue == null) {
                return Result.error("缺少必填参数");
            }

            EnterpriseAuditImage audit = imageAuditService.submitAudit(enterpriseId, userId, fieldName, oldValue, newValue);
            return Result.success("图片变更审核已提交，请等待管理员审核", audit);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理员查询图片审核列表
     */
    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer auditStatus) {
        Page<Map<String, Object>> page = imageAuditService.getAuditList(current, size, auditStatus);
        return Result.success(page);
    }

    /**
     * 管理员审核图片变更
     */
    @OperationLog(module = "图片审核", operation = "审核图片变更")
    @PostMapping("/approve")
    public Result<Void> approve(@RequestBody Map<String, Object> body,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long auditId = body.get("auditId") != null ? Long.parseLong(body.get("auditId").toString()) : null;
            Boolean approved = body.get("approved") != null ? Boolean.parseBoolean(body.get("approved").toString()) : null;
            String remark = (String) body.get("remark");

            if (auditId == null || approved == null) {
                return Result.error("缺少必填参数");
            }

            Long adminId = null;
            if (token != null) {
                try {
                    if (token.startsWith("Bearer ")) token = token.substring(7);
                    adminId = jwtUtils.getUserIdFromToken(token);
                } catch (Exception ignored) {}
            }

            imageAuditService.approveAudit(auditId, approved, remark, adminId);
            return Result.success(approved ? "审核通过" : "审核已拒绝", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询企业的待审核图片数量
     */
    @GetMapping("/pending-count")
    public Result<Long> pendingCount(@RequestParam Long enterpriseId) {
        long count = imageAuditService.getPendingCount(enterpriseId);
        return Result.success(count);
    }

    /**
     * 查询企业的图片审核记录
     */
    @GetMapping("/qualification-pending-count")
    public Result<Long> qualificationPendingCount() {
        long count = imageAuditService.getQualificationPendingCount();
        return Result.success(count);
    }

    @GetMapping("/by-enterprise")
    public Result<List<EnterpriseAuditImage>> byEnterprise(@RequestParam Long enterpriseId) {
        List<EnterpriseAuditImage> list = imageAuditService.getByEnterprise(enterpriseId);
        return Result.success(list);
    }

    /**
     * 聚合所有业务表图片（管理员用）
     */
    @GetMapping("/business-images")
    public Result<Map<String, Object>> businessImages(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size) {
        Map<String, Object> data = imageAuditService.getAllBusinessImages(current, size);
        return Result.success(data);
    }

    /**
     * 业务图片违规标记
     */
    @OperationLog(module = "图片审核", operation = "标记业务图片违规")
    @PostMapping("/business-violation")
    public Result<Void> businessViolation(@RequestBody Map<String, Object> body) {
        try {
            String sourceType = (String) body.get("sourceType");
            Long sourceId = body.get("sourceId") != null ? Long.parseLong(body.get("sourceId").toString()) : null;
            String imageUrl = (String) body.get("imageUrl");
            if (sourceType == null || sourceId == null || imageUrl == null) {
                return Result.error("缺少必填参数");
            }
            imageAuditService.markBusinessImageViolation(sourceType, sourceId, imageUrl);
            return Result.success("已标记违规", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 业务图片删除
     */
    @OperationLog(module = "图片审核", operation = "删除业务图片")
    @PostMapping("/business-delete")
    public Result<Void> businessDelete(@RequestBody Map<String, Object> body) {
        try {
            String sourceType = (String) body.get("sourceType");
            Long sourceId = body.get("sourceId") != null ? Long.parseLong(body.get("sourceId").toString()) : null;
            String imageUrl = (String) body.get("imageUrl");
            if (sourceType == null || sourceId == null || imageUrl == null) {
                return Result.error("缺少必填参数");
            }
            imageAuditService.deleteBusinessImage(sourceType, sourceId, imageUrl);
            return Result.success("已删除", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 资质图片违规标记
     */
    @OperationLog(module = "图片审核", operation = "标记资质图片违规")
    @PostMapping("/qualification-violation")
    public Result<Void> qualificationViolation(@RequestBody Map<String, Object> body) {
        try {
            Long auditId = body.get("auditId") != null ? Long.parseLong(body.get("auditId").toString()) : null;
            if (auditId == null) return Result.error("缺少必填参数");
            imageAuditService.markQualificationViolation(auditId);
            return Result.success("已标记违规", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 资质图片审核记录删除
     */
    @OperationLog(module = "图片审核", operation = "删除资质图片审核记录")
    @PostMapping("/qualification-delete")
    public Result<Void> qualificationDelete(@RequestBody Map<String, Object> body) {
        try {
            Long auditId = body.get("auditId") != null ? Long.parseLong(body.get("auditId").toString()) : null;
            if (auditId == null) return Result.error("缺少必填参数");
            imageAuditService.deleteQualificationAudit(auditId);
            return Result.success("已删除", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
