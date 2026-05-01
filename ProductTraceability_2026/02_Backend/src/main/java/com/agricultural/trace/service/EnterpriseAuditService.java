package com.agricultural.trace.service;

import com.agricultural.trace.dto.EnterpriseAuditDTO;
import com.agricultural.trace.entity.EnterpriseInfo;
import com.agricultural.trace.entity.SysUser;
import com.agricultural.trace.mapper.EnterpriseInfoMapper;
import com.agricultural.trace.mapper.SysUserMapper;
import com.agricultural.trace.vo.EnterpriseAuditVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业入驻审核服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseAuditService {

    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final MessageService messageService;

    public Page<EnterpriseAuditVO> getAuditList(Long current, Long size,
                                                Integer enterpriseType, Integer auditStatus, String keyword) {
        Page<EnterpriseInfo> page = new Page<>(current, size);

        LambdaQueryWrapper<EnterpriseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(enterpriseType != null, EnterpriseInfo::getEnterpriseType, enterpriseType);
        wrapper.eq(auditStatus != null, EnterpriseInfo::getAuditStatus, auditStatus);
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(w -> w.like(EnterpriseInfo::getEnterpriseName, trimmedKeyword)
                    .or().like(EnterpriseInfo::getEnterpriseCode, trimmedKeyword)
                    .or().like(EnterpriseInfo::getContactPerson, trimmedKeyword));
        }
        wrapper.orderByDesc(EnterpriseInfo::getCreateTime);

        Page<EnterpriseInfo> enterprisePage = enterpriseInfoMapper.selectPage(page, wrapper);

        Page<EnterpriseAuditVO> voPage = new Page<>(current, size);
        voPage.setTotal(enterprisePage.getTotal());
        voPage.setPages(enterprisePage.getPages());
        voPage.setRecords(enterprisePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    public EnterpriseAuditVO getAuditDetail(Long id) {
        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(id);
        if (enterprise == null) {
            throw new RuntimeException("企业不存在");
        }
        return convertToVO(enterprise);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveEnterprise(EnterpriseAuditDTO dto) {
        Long enterpriseId = dto.getEnterpriseId();
        Integer auditStatus = dto.getAuditStatus();
        String auditRemark = dto.getAuditRemark();

        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new RuntimeException("企业不存在");
        }

        if (enterprise.getEnterpriseType() == null
                || enterprise.getEnterpriseType() < 1
                || enterprise.getEnterpriseType() > 3) {
            throw new RuntimeException("企业类型不正确，请核实");
        }

        enterprise.setAuditStatus(auditStatus);
        enterprise.setAuditRemark(auditRemark);
        enterprise.setAuditTime(LocalDateTime.now());
        enterprise.setAuditBy(getCurrentUserId());
        enterpriseInfoMapper.updateById(enterprise);

        if (auditStatus == 1) {
            syncEnterpriseUserContext(enterprise);
            log.info("企业审核通过，已同步企业账号上下文，enterpriseId: {}, enterpriseType: {}",
                    enterpriseId, enterprise.getEnterpriseType());

            if (enterprise.getCreateBy() != null) {
                messageService.sendMessage(enterprise.getCreateBy(), "business", "企业入驻审核通过",
                        "您申请入驻的企业\"" + enterprise.getEnterpriseName() + "\"已通过审核",
                        "恭喜！企业入驻审核通过\n\n企业名称：" + enterprise.getEnterpriseName()
                                + "\n审核时间：" + LocalDateTime.now()
                                + "\n审核结果：通过\n\n您现在可以使用工作台的所有功能，开始进行溯源数据的上链操作。",
                        "/pages/workbench/workbench", "前往工作台", "开始使用溯源功能");
            }
            return;
        }

        syncApplicantUserContext(enterprise);
        log.info("企业审核拒绝，enterpriseId: {}, reason: {}", enterpriseId, auditRemark);

        if (enterprise.getCreateBy() != null) {
            String rejectReason = StringUtils.hasText(auditRemark) ? auditRemark : "未提供原因";
            messageService.sendMessage(enterprise.getCreateBy(), "business", "企业入驻审核未通过",
                    "您申请入驻的企业\"" + enterprise.getEnterpriseName() + "\"未通过审核",
                    "很遗憾，您的企业入驻申请未通过审核。\n\n企业名称：" + enterprise.getEnterpriseName()
                            + "\n审核时间：" + LocalDateTime.now()
                            + "\n拒绝原因：" + rejectReason
                            + "\n\n请根据拒绝原因修改后重新提交申请。如有疑问，请联系管理员。",
                    null, null, null);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchApprove(Long[] ids, Integer auditStatus, String auditRemark) {
        Arrays.stream(ids).forEach(id -> {
            EnterpriseAuditDTO dto = new EnterpriseAuditDTO();
            dto.setEnterpriseId(id);
            dto.setAuditStatus(auditStatus);
            dto.setAuditRemark(auditRemark);
            approveEnterprise(dto);
        });
    }

    private void syncEnterpriseUserContext(EnterpriseInfo enterprise) {
        Long userId = enterprise.getCreateBy();
        if (userId == null) {
            return;
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            log.warn("企业审核通过时未找到创建账号, enterpriseId={}, userId={}", enterprise.getId(), userId);
            return;
        }

        user.setUserType(2);
        user.setEnterpriseId(enterprise.getId());
        user.setEnterpriseType(enterprise.getEnterpriseType());
        user.setStatus(1);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    private void syncApplicantUserContext(EnterpriseInfo enterprise) {
        Long userId = enterprise.getCreateBy();
        if (userId == null) {
            return;
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            log.warn("企业审核拒绝时未找到创建账号, enterpriseId={}, userId={}", enterprise.getId(), userId);
            return;
        }

        user.setUserType(3);
        user.setEnterpriseId(null);
        user.setEnterpriseType(null);
        user.setStatus(1);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
                .set(SysUser::getUserType, 3)
                .set(SysUser::getEnterpriseId, null)
                .set(SysUser::getEnterpriseType, null)
                .set(SysUser::getStatus, 1)
                .set(SysUser::getUpdateTime, LocalDateTime.now());
        sysUserMapper.update(null, wrapper);
    }

    private Long getCurrentUserId() {
        return 1L;
    }

    private static final Map<Integer, String> TYPE_NAME_MAP = Map.of(
            1, "种植养殖", 2, "加工宰杀", 3, "检疫质检");
    private static final Map<Integer, String> STATUS_NAME_MAP = Map.of(
            0, "待审核", 1, "已通过", 2, "已驳回");

    private EnterpriseAuditVO convertToVO(EnterpriseInfo e) {
        EnterpriseAuditVO vo = new EnterpriseAuditVO();
        vo.setId(e.getId());
        vo.setEnterpriseCode(e.getEnterpriseCode());
        vo.setEnterpriseName(e.getEnterpriseName());
        vo.setEnterpriseType(e.getEnterpriseType());
        vo.setEnterpriseTypeName(TYPE_NAME_MAP.getOrDefault(e.getEnterpriseType(), "未知"));
        vo.setLegalPerson(e.getLegalPerson());
        vo.setContactPerson(e.getContactPerson());
        vo.setContactPhone(e.getContactPhone());
        vo.setContactEmail(e.getContactEmail());
        vo.setAddress(e.getAddress());
        vo.setProvince(e.getProvince());
        vo.setCity(e.getCity());
        vo.setDistrict(e.getDistrict());
        vo.setIntroduction(e.getIntroduction());
        vo.setOtherCertificates(e.getOtherCertificates());
        vo.setBusinessLicense(e.getBusinessLicense());
        vo.setProductionLicense(e.getProductionLicense());
        vo.setAuditStatus(e.getAuditStatus());
        vo.setAuditStatusName(STATUS_NAME_MAP.getOrDefault(e.getAuditStatus(), "未知"));
        vo.setAuditRemark(e.getAuditRemark());
        vo.setAuditTime(e.getAuditTime());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }
}
