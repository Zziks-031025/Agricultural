package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.dto.EnterpriseAuditDTO;
import com.agricultural.trace.service.EnterpriseAuditService;
import com.agricultural.trace.vo.EnterpriseAuditVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业入驻审核控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/enterprise/audit")
@RequiredArgsConstructor
@CrossOrigin
public class EnterpriseAuditController {

    private final EnterpriseAuditService enterpriseAuditService;

    /**
     * 分页查询待审核企业列表
     */
    @GetMapping("/list")
    public Result<Page<EnterpriseAuditVO>> getAuditList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer enterpriseType,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String keyword
    ) {
        log.info("查询企业审核列表, current: {}, size: {}, enterpriseType: {}, auditStatus: {}, keyword: {}",
                current, size, enterpriseType, auditStatus, keyword);

        Page<EnterpriseAuditVO> page = enterpriseAuditService.getAuditList(
                current, size, enterpriseType, auditStatus, keyword
        );

        return Result.success(page);
    }

    /**
     * 查询企业审核详情
     */
    @GetMapping("/detail/{id}")
    public Result<EnterpriseAuditVO> getAuditDetail(@PathVariable Long id) {
        log.info("查询企业审核详情, id: {}", id);
        return Result.success(enterpriseAuditService.getAuditDetail(id));
    }

    /**
     * 审核企业入驻申请
     */
    @OperationLog(module = "企业审核", operation = "审核企业入驻")
    @PostMapping("/approve")
    public Result<String> approveEnterprise(@Validated @RequestBody EnterpriseAuditDTO dto) {
        log.info("审核企业入驻申请, enterpriseId: {}, auditStatus: {}", dto.getEnterpriseId(), dto.getAuditStatus());
        enterpriseAuditService.approveEnterprise(dto);
        return Result.success("审核完成");
    }

    /**
     * 批量审核企业
     */
    @OperationLog(module = "企业审核", operation = "批量审核企业")
    @PostMapping("/batch-approve")
    public Result<String> batchApprove(
            @RequestParam Long[] ids,
            @RequestParam Integer auditStatus,
            @RequestParam(required = false) String auditRemark
    ) {
        log.info("批量审核企业, ids: {}, auditStatus: {}", ids, auditStatus);
        enterpriseAuditService.batchApprove(ids, auditStatus, auditRemark);
        return Result.success("批量审核完成");
    }
}
