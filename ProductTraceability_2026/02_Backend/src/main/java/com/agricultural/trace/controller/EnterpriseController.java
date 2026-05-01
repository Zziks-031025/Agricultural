package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.config.FileUploadConfig;
import com.agricultural.trace.entity.EnterpriseInfo;
import com.agricultural.trace.entity.SysUser;
import com.agricultural.trace.mapper.EnterpriseInfoMapper;
import com.agricultural.trace.mapper.SysUserMapper;
import com.agricultural.trace.service.DashboardService;
import com.agricultural.trace.service.MessageService;
import com.agricultural.trace.service.SystemUserService;
import com.agricultural.trace.util.FileCleanupUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 企业信息控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
@CrossOrigin
public class EnterpriseController {

    private static final Pattern MOBILE_PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final FileUploadConfig fileUploadConfig;
    private final SystemUserService systemUserService;
    private final MessageService messageService;
    private final DashboardService dashboardService;
    private final JdbcTemplate jdbcTemplate;

    @OperationLog("企业入驻注册")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, Object> body) {
        try {
            String enterpriseName = (String) body.get("enterpriseName");
            String enterpriseCode = (String) body.get("enterpriseCode");
            Integer enterpriseType = body.get("enterpriseType") != null ? ((Number) body.get("enterpriseType")).intValue() : null;
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String contactPhone = (String) body.get("contactPhone");
            Long existingUserId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;

            if (!StringUtils.hasText(enterpriseName) || !StringUtils.hasText(enterpriseCode)
                    || enterpriseType == null) {
                return Result.error("必填字段不能为空");
            }
            // 未传 userId 时需要 username/password 创建新用户
            if (existingUserId == null && (!StringUtils.hasText(username) || !StringUtils.hasText(password))) {
                return Result.error("账号和密码不能为空");
            }
            if (StringUtils.hasText(contactPhone) && !isValidMobilePhone(contactPhone)) {
                return Result.error("联系电话格式不正确，应为11位手机号");
            }

            LambdaQueryWrapper<EnterpriseInfo> codeCheck = new LambdaQueryWrapper<>();
            codeCheck.eq(EnterpriseInfo::getEnterpriseCode, enterpriseCode);
            if (enterpriseInfoMapper.selectCount(codeCheck) > 0) {
                return Result.error("该统一社会信用代码已被注册");
            }

            EnterpriseInfo enterprise = new EnterpriseInfo();
            enterprise.setEnterpriseName(enterpriseName);
            enterprise.setEnterpriseCode(enterpriseCode);
            enterprise.setEnterpriseType(enterpriseType);
            enterprise.setLegalPerson((String) body.get("legalPerson"));
            enterprise.setContactPerson((String) body.get("contactPerson"));
            enterprise.setContactPhone(contactPhone);
            enterprise.setContactEmail((String) body.get("contactEmail"));
            enterprise.setProvince((String) body.get("province"));
            enterprise.setCity((String) body.get("city"));
            enterprise.setDistrict((String) body.get("district"));
            enterprise.setAddress((String) body.get("address"));
            enterprise.setBusinessLicense((String) body.get("businessLicense"));
            enterprise.setProductionLicense((String) body.get("productionLicense"));
            enterprise.setIntroduction((String) body.get("introduction"));
            enterprise.setLogo((String) body.get("logo"));
            enterprise.setCoverImage((String) body.get("coverImage"));
            enterprise.setAuditStatus(0);
            enterprise.setStatus(1);
            enterprise.setCreateTime(LocalDateTime.now());
            enterprise.setUpdateTime(LocalDateTime.now());

            Long userId;
            if (existingUserId != null) {
                // 已登录用户入驻：绑定已有用户，不创建新用户
                SysUser existingUser = sysUserMapper.selectById(existingUserId);
                if (existingUser == null) {
                    return Result.error("用户不存在");
                }
                if (existingUser.getEnterpriseId() != null) {
                    return Result.error("该用户已关联企业，不能重复申请");
                }
                userId = existingUserId;
                enterprise.setCreateBy(userId);
                enterpriseInfoMapper.insert(enterprise);
            } else {
                // 未登录用户入驻：创建新用户
                LambdaQueryWrapper<SysUser> userCheck = new LambdaQueryWrapper<>();
                userCheck.eq(SysUser::getUsername, username);
                if (sysUserMapper.selectCount(userCheck) > 0) {
                    return Result.error("该用户名已被注册");
                }

                enterpriseInfoMapper.insert(enterprise);

                SysUser user = new SysUser();
                user.setUsername(username);
                user.setRealName((String) body.get("contactPerson"));
                user.setPhone(contactPhone);
                user.setEmail((String) body.get("contactEmail"));
                user.setUserType(3);
                user.setEnterpriseId(null);
                user.setStatus(1);
                systemUserService.addUser(user, password, Collections.emptyList());
                userId = user.getId();

                enterprise.setCreateBy(userId);
                enterpriseInfoMapper.updateById(enterprise);
            }

            messageService.sendMessage(
                    userId,
                    "system",
                    "入驻申请已提交",
                    "您的企业入驻申请已提交，请等待管理员审核",
                    "尊敬的用户，\n\n您的企业入驻申请已成功提交。\n\n企业名称：" + enterpriseName
                            + "\n企业编码：" + enterpriseCode
                            + "\n\n管理员将在1-3个工作日内完成审核，审核结果将通过消息通知您。",
                    null,
                    null,
                    null
            );
            messageService.sendToAdmins(
                    "business",
                    "企业入驻待审核",
                    "企业「" + enterpriseName + "」提交了新的入驻申请",
                    "企业名称：" + enterpriseName
                            + "\n企业编码：" + enterpriseCode
                            + "\n企业类型：" + enterpriseType
                            + "\n联系人：" + nullToEmpty((String) body.get("contactPerson"))
                            + "\n联系电话：" + nullToEmpty(contactPhone)
                            + "\n请及时前往企业审核页面处理。",
                    "/enterprise/audit",
                    "前往审核",
                    "查看企业入驻申请"
            );

            log.info("企业入驻注册成功, enterprise={}, user={}", enterpriseName, username);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("企业入驻注册失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("企业入驻注册异常: {}", e.getMessage());
            return Result.error("注册失败，请稍后重试");
        }
    }

    @GetMapping("/list-by-type")
    public Result<java.util.List<EnterpriseInfo>> listByType(@RequestParam Integer type) {
        LambdaQueryWrapper<EnterpriseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnterpriseInfo::getEnterpriseType, type)
                .eq(EnterpriseInfo::getAuditStatus, 1)
                .eq(EnterpriseInfo::getStatus, 1)
                .orderByAsc(EnterpriseInfo::getEnterpriseName);
        return Result.success(enterpriseInfoMapper.selectList(wrapper));
    }

    @GetMapping("/list")
    public Result<Page<EnterpriseInfo>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String enterpriseName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer enterpriseType,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<EnterpriseInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(enterpriseName)) {
            wrapper.like(EnterpriseInfo::getEnterpriseName, enterpriseName);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(EnterpriseInfo::getEnterpriseName, keyword)
                    .or().like(EnterpriseInfo::getEnterpriseCode, keyword)
                    .or().like(EnterpriseInfo::getContactPerson, keyword));
        }
        if (enterpriseType != null) {
            wrapper.eq(EnterpriseInfo::getEnterpriseType, enterpriseType);
        }
        if (auditStatus != null) {
            wrapper.eq(EnterpriseInfo::getAuditStatus, auditStatus);
        }
        if (status != null) {
            wrapper.eq(EnterpriseInfo::getStatus, status);
        }
        wrapper.orderByDesc(EnterpriseInfo::getCreateTime);

        Page<EnterpriseInfo> page = enterpriseInfoMapper.selectPage(new Page<>(current, size), wrapper);
        page.getRecords().forEach(this::attachEnterpriseMetrics);
        return Result.success(page);
    }

    @OperationLog("切换企业状态")
    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(id);
        if (enterprise == null) {
            return Result.error("企业不存在");
        }
        enterprise.setStatus(status);
        enterprise.setUpdateTime(LocalDateTime.now());
        enterpriseInfoMapper.updateById(enterprise);
        log.info("企业状态变更id={}, status={}", id, status);
        return Result.success();
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(id);
        if (enterprise == null) {
            return Result.error("企业不存在");
        }
        return Result.success(buildEnterpriseDetailPayload(enterprise));
    }

    @GetMapping("/metrics/{id}")
    public Result<Map<String, Object>> getMetrics(@PathVariable Long id) {
        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(id);
        if (enterprise == null) {
            return Result.error("企业不存在");
        }
        return Result.success(buildEnterpriseMetricsPayload(enterprise));
    }

    @OperationLog("更新企业信息")
    @PutMapping("/update")
    public Result<Void> updateEnterprise(@RequestBody EnterpriseInfo info) {
        log.info("更新企业信息, id={}", info.getId());

        if (info.getId() == null) {
            return Result.error("企业ID不能为空");
        }
        if (StringUtils.hasText(info.getContactPhone()) && !isValidMobilePhone(info.getContactPhone())) {
            return Result.error("联系电话格式不正确，应为11位手机号");
        }

        EnterpriseInfo existing = enterpriseInfoMapper.selectById(info.getId());
        if (existing == null) {
            return Result.error("企业不存在");
        }

        String uploadDir = fileUploadConfig.getUploadDir();
        FileCleanupUtil.deleteIfChanged(uploadDir, existing.getBusinessLicense(), info.getBusinessLicense());
        FileCleanupUtil.deleteIfChanged(uploadDir, existing.getProductionLicense(), info.getProductionLicense());
        FileCleanupUtil.deleteIfChanged(uploadDir, existing.getLogo(), info.getLogo());
        FileCleanupUtil.deleteIfChanged(uploadDir, existing.getCoverImage(), info.getCoverImage());

        existing.setEnterpriseName(info.getEnterpriseName());
        existing.setLegalPerson(info.getLegalPerson());
        existing.setContactPerson(info.getContactPerson());
        existing.setContactPhone(info.getContactPhone());
        existing.setContactEmail(info.getContactEmail());
        existing.setProvince(info.getProvince());
        existing.setCity(info.getCity());
        existing.setDistrict(info.getDistrict());
        existing.setAddress(info.getAddress());
        existing.setIntroduction(info.getIntroduction());
        existing.setBusinessLicense(info.getBusinessLicense());
        existing.setProductionLicense(info.getProductionLicense());
        existing.setOtherCertificates(info.getOtherCertificates());
        existing.setLogo(info.getLogo());
        existing.setCoverImage(info.getCoverImage());
        existing.setUpdateTime(LocalDateTime.now());

        enterpriseInfoMapper.updateById(existing);
        log.info("企业信息更新成功, id={}", info.getId());
        return Result.success();
    }

    @OperationLog("重新提交企业入驻审核")
    @PutMapping("/reapply/{id}")
    public Result<Void> reapplyEnterprise(@PathVariable Long id) {
        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(id);
        if (enterprise == null) {
            return Result.error("企业不存在");
        }
        if (!Integer.valueOf(2).equals(enterprise.getAuditStatus())) {
            return Result.error("仅已驳回企业可以重新提交审核");
        }

        enterprise.setAuditStatus(0);
        enterprise.setAuditRemark(null);
        enterprise.setAuditTime(null);
        enterprise.setAuditBy(null);
        enterprise.setUpdateTime(LocalDateTime.now());
        enterpriseInfoMapper.updateById(enterprise);
        resetApplicantUserContext(enterprise.getCreateBy());

        if (enterprise.getCreateBy() != null) {
            messageService.sendMessage(
                    enterprise.getCreateBy(),
                    "system",
                    "企业入驻已重新提交",
                    "您的企业入驻申请已重新提交，请等待管理员审核",
                    "企业名称：" + enterprise.getEnterpriseName()
                            + "\n重新提交时间：" + LocalDateTime.now()
                            + "\n请等待管理员再次审核。",
                    null,
                    null,
                    null
            );
        }
        messageService.sendToAdmins(
                "business",
                "企业入驻重新提交审核",
                "企业「" + enterprise.getEnterpriseName() + "」重新提交了入驻申请",
                "企业名称：" + enterprise.getEnterpriseName()
                        + "\n企业编码：" + enterprise.getEnterpriseCode()
                        + "\n请及时前往企业审核页面处理。",
                "/enterprise/audit",
                "前往审核",
                "查看重新提交的企业入驻申请"
        );
        return Result.success();
    }

    @OperationLog("删除已驳回入驻申请")
    @DeleteMapping("/delete-rejected/{id}")
    public Result<Void> deleteRejectedEnterprise(@PathVariable Long id) {
        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(id);
        if (enterprise == null) {
            return Result.error("企业不存在");
        }
        if (!Integer.valueOf(2).equals(enterprise.getAuditStatus())) {
            return Result.error("仅已驳回企业可以删除申请");
        }

        if (enterprise.getCreateBy() != null) {
            sysUserMapper.deleteById(enterprise.getCreateBy());
        }
        enterpriseInfoMapper.deleteById(id);
        return Result.success();
    }

    private boolean isValidMobilePhone(String phone) {
        return StringUtils.hasText(phone) && MOBILE_PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void resetApplicantUserContext(Long userId) {
        if (userId == null) {
            return;
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            user.setUserType(3);
            user.setEnterpriseId(null);
            user.setEnterpriseType(null);
            user.setStatus(1);
            user.setUpdateTime(LocalDateTime.now());
            sysUserMapper.updateById(user);
        }

        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
                .set(SysUser::getUserType, 3)
                .set(SysUser::getEnterpriseId, null)
                .set(SysUser::getEnterpriseType, null)
                .set(SysUser::getStatus, 1)
                .set(SysUser::getUpdateTime, LocalDateTime.now());
        sysUserMapper.update(null, wrapper);
    }

    private Map<String, Object> buildEnterpriseDetailPayload(EnterpriseInfo enterprise) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", enterprise.getId());
        payload.put("enterpriseName", enterprise.getEnterpriseName());
        payload.put("enterpriseCode", enterprise.getEnterpriseCode());
        payload.put("enterpriseType", enterprise.getEnterpriseType());
        payload.put("legalPerson", enterprise.getLegalPerson());
        payload.put("contactPerson", enterprise.getContactPerson());
        payload.put("contactPhone", enterprise.getContactPhone());
        payload.put("contactEmail", enterprise.getContactEmail());
        payload.put("province", enterprise.getProvince());
        payload.put("city", enterprise.getCity());
        payload.put("district", enterprise.getDistrict());
        payload.put("address", enterprise.getAddress());
        payload.put("businessLicense", enterprise.getBusinessLicense());
        payload.put("productionLicense", enterprise.getProductionLicense());
        payload.put("otherCertificates", enterprise.getOtherCertificates());
        payload.put("introduction", enterprise.getIntroduction());
        payload.put("logo", enterprise.getLogo());
        payload.put("coverImage", enterprise.getCoverImage());
        payload.put("auditStatus", enterprise.getAuditStatus());
        payload.put("auditRemark", enterprise.getAuditRemark());
        payload.put("auditTime", enterprise.getAuditTime());
        payload.put("status", enterprise.getStatus());
        payload.put("createBy", enterprise.getCreateBy());
        payload.put("updateBy", enterprise.getUpdateBy());
        payload.put("createTime", enterprise.getCreateTime());
        payload.put("updateTime", enterprise.getUpdateTime());

        payload.putAll(buildEnterpriseMetricsPayload(enterprise));
        return payload;
    }

    private Map<String, Object> buildEnterpriseMetricsPayload(EnterpriseInfo enterprise) {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> stats = resolveEnterpriseStats(enterprise);
        payload.put("chainCount", toLong(stats.get("totalOnChain")));
        payload.put("batchCount", toLong(stats.get("batchCount")));
        payload.put("verifyCount", queryVerifyCount(enterprise != null ? enterprise.getId() : null));
        payload.put("productCount", queryProductCount(enterprise != null ? enterprise.getId() : null));
        payload.put("lastChainTime", queryLastChainTime(enterprise != null ? enterprise.getId() : null));
        return payload;
    }

    private Map<String, Object> resolveEnterpriseStats(EnterpriseInfo enterprise) {
        if (enterprise == null || enterprise.getId() == null || enterprise.getEnterpriseType() == null) {
            return Collections.emptyMap();
        }
        switch (enterprise.getEnterpriseType()) {
            case 1:
                return dashboardService.getFarmerStats(enterprise.getId());
            case 2:
                return dashboardService.getProcessorStats(enterprise.getId());
            case 3:
                return dashboardService.getQuarantineStats(enterprise.getId());
            default:
                return Collections.emptyMap();
        }
    }

    private long queryVerifyCount(Long enterpriseId) {
        if (enterpriseId == null) {
            return 0L;
        }
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_scan_log l " +
                            "LEFT JOIN trace_batch b ON (l.batch_id = b.id OR (l.batch_id IS NULL AND l.batch_code = b.batch_code)) " +
                            "WHERE b.enterprise_id = ? OR b.receive_enterprise_id = ?",
                    Long.class,
                    enterpriseId,
                    enterpriseId
            );
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private void attachEnterpriseMetrics(EnterpriseInfo enterprise) {
        if (enterprise == null || enterprise.getId() == null) {
            return;
        }
        Map<String, Object> stats = resolveEnterpriseStats(enterprise);
        enterprise.setChainCount(toLong(stats.get("totalOnChain")));
        enterprise.setBatchCount(toLong(stats.get("batchCount")));
        enterprise.setVerifyCount(queryVerifyCount(enterprise.getId()));
        enterprise.setProductCount(queryProductCount(enterprise.getId()));
        enterprise.setLastChainTime(queryLastChainTime(enterprise.getId()));
    }

    private long queryProductCount(Long enterpriseId) {
        if (enterpriseId == null) {
            return 0L;
        }
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT product_name) FROM trace_batch WHERE enterprise_id = ? OR receive_enterprise_id = ?",
                    Long.class,
                    enterpriseId,
                    enterpriseId
            );
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String queryLastChainTime(Long enterpriseId) {
        if (enterpriseId == null) {
            return null;
        }
        try {
            LocalDateTime lastChainTime = jdbcTemplate.queryForObject(
                    "SELECT MAX(chain_time) FROM (" +
                            " SELECT chain_time FROM trace_batch WHERE (enterprise_id = ? OR receive_enterprise_id = ?) AND tx_hash IS NOT NULL AND tx_hash != ''" +
                            " UNION ALL SELECT t.chain_time FROM trace_record t JOIN trace_batch b ON t.batch_id = b.id WHERE (b.enterprise_id = ? OR b.receive_enterprise_id = ?) AND t.tx_hash IS NOT NULL AND t.tx_hash != ''" +
                            " UNION ALL SELECT t.chain_time FROM trace_inspection t JOIN trace_batch b ON t.batch_id = b.id WHERE (b.enterprise_id = ? OR t.inspection_enterprise_id = ?) AND t.tx_hash IS NOT NULL AND t.tx_hash != ''" +
                            " UNION ALL SELECT chain_time FROM trace_processing WHERE processing_enterprise_id = ? AND tx_hash IS NOT NULL AND tx_hash != ''" +
                            " UNION ALL SELECT chain_time FROM trace_storage WHERE storage_enterprise_id = ? AND tx_hash IS NOT NULL AND tx_hash != ''" +
                            " UNION ALL SELECT chain_time FROM trace_transport WHERE transport_enterprise_id = ? AND tx_hash IS NOT NULL AND tx_hash != ''" +
                            " UNION ALL SELECT chain_time FROM trace_sale WHERE sale_enterprise_id = ? AND tx_hash IS NOT NULL AND tx_hash != ''" +
                            ") chain_union",
                    LocalDateTime.class,
                    enterpriseId, enterpriseId,
                    enterpriseId, enterpriseId,
                    enterpriseId, enterpriseId,
                    enterpriseId,
                    enterpriseId,
                    enterpriseId,
                    enterpriseId
            );
            return lastChainTime != null ? lastChainTime.format(DATETIME_FORMATTER) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
