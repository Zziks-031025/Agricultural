package com.agricultural.trace.service;

import com.agricultural.trace.entity.*;
import com.agricultural.trace.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 企业图片审核服务
 * 处理头像、营业执照、行业许可证、企业logo的变更审核
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageAuditService {

    private final EnterpriseAuditImageMapper auditImageMapper;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final MessageService messageService;
    private final TraceTransportMapper transportMapper;
    private final TraceStorageMapper storageMapper;
    private final TraceSaleMapper saleMapper;
    private final TraceProcessingMapper processingMapper;
    private final TraceInspectionMapper inspectionMapper;
    private final TraceBatchMapper batchMapper;

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 需要审核的字段集合 */
    private static final Set<String> AUDITABLE_FIELDS = new HashSet<>(Arrays.asList(
            "avatar", "business_license", "production_license", "logo", "cover_image"
    ));

    /**
     * 判断字段是否需要审核
     */
    public static boolean needsAudit(String fieldName) {
        return AUDITABLE_FIELDS.contains(fieldName);
    }

    /**
     * 提交图片变更审核申请
     */
    @Transactional(rollbackFor = Exception.class)
    public EnterpriseAuditImage submitAudit(Long enterpriseId, Long userId, String fieldName,
                                             String oldValue, String newValue) {
        if (!needsAudit(fieldName)) {
            throw new RuntimeException("该字段不需要审核: " + fieldName);
        }

        // 检查是否有该字段的待审核记录，如有则替换
        LambdaQueryWrapper<EnterpriseAuditImage> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(EnterpriseAuditImage::getEnterpriseId, enterpriseId)
                    .eq(EnterpriseAuditImage::getFieldName, fieldName)
                    .eq(EnterpriseAuditImage::getAuditStatus, 0);
        EnterpriseAuditImage existing = auditImageMapper.selectOne(existWrapper);

        if (existing != null) {
            existing.setNewValue(newValue);
            existing.setUpdateTime(LocalDateTime.now());
            auditImageMapper.updateById(existing);
            sendPendingAuditNotification(enterpriseId, fieldLabel(fieldName));
            sendAdminPendingAuditNotification(enterpriseId, fieldName);
            log.info("更新已有待审核记录, id={}, field={}", existing.getId(), fieldName);
            return existing;
        }

        EnterpriseAuditImage audit = new EnterpriseAuditImage();
        audit.setEnterpriseId(enterpriseId);
        audit.setUserId(userId);
        audit.setFieldName(fieldName);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setAuditStatus(0);
        audit.setCreateTime(LocalDateTime.now());
        audit.setUpdateTime(LocalDateTime.now());
        auditImageMapper.insert(audit);

        sendPendingAuditNotification(enterpriseId, fieldLabel(fieldName));
        sendAdminPendingAuditNotification(enterpriseId, fieldName);
        log.info("提交图片审核申请, enterpriseId={}, field={}, auditId={}", enterpriseId, fieldName, audit.getId());
        return audit;
    }

    private void sendPendingAuditNotification(Long enterpriseId, String fieldLabel) {
        messageService.sendToEnterprise(enterpriseId, "system",
                "图片变更审核中",
                "您提交的「" + fieldLabel + "」图片变更已进入审核流程",
                "您提交的「" + fieldLabel + "」图片变更正在审核中，审核通过后将自动生效。",
                null, null, null);
    }

    /**
     * 管理员查询图片审核列表（分页）
     */
    private void sendAdminPendingAuditNotification(Long enterpriseId, String fieldName) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserType, 1)
                .eq(SysUser::getStatus, 1);
        List<SysUser> admins = sysUserMapper.selectList(wrapper);
        if (admins == null || admins.isEmpty()) {
            return;
        }

        EnterpriseInfo enterprise = enterpriseId != null ? enterpriseInfoMapper.selectById(enterpriseId) : null;
        String enterpriseName = enterprise != null && enterprise.getEnterpriseName() != null
                ? enterprise.getEnterpriseName() : "未知企业";
        String label = fieldLabel(fieldName);
        String summary = enterpriseName + "提交了" + label + "图片变更，请及时审核";
        String content = "企业名称：" + enterpriseName
                + "\n变更类型：" + label
                + "\n说明：企业提交了新的图片变更申请，请尽快前往图片审核管理处理。";

        for (SysUser admin : admins) {
            messageService.sendMessage(admin.getId(), "business",
                    "企业图片审核待处理",
                    summary,
                    content,
                    "/enterprise/image-audit",
                    "立即处理",
                    label + "图片变更待审核");
        }
    }

    public Page<Map<String, Object>> getAuditList(Long current, Long size, Integer auditStatus) {
        Page<EnterpriseAuditImage> page = new Page<>(current, size);
        LambdaQueryWrapper<EnterpriseAuditImage> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) {
            wrapper.eq(EnterpriseAuditImage::getAuditStatus, auditStatus);
        }
        wrapper.orderByAsc(EnterpriseAuditImage::getAuditStatus);
        wrapper.orderByDesc(EnterpriseAuditImage::getCreateTime);

        Page<EnterpriseAuditImage> result = auditImageMapper.selectPage(page, wrapper);

        // 组装返回数据（补充企业名称、用户名称、字段中文名）
        Page<Map<String, Object>> voPage = new Page<>(current, size, result.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (EnterpriseAuditImage item : result.getRecords()) {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", item.getId());
            vo.put("enterpriseId", item.getEnterpriseId());
            vo.put("userId", item.getUserId());
            vo.put("fieldName", item.getFieldName());
            vo.put("fieldLabel", fieldLabel(item.getFieldName()));
            vo.put("oldValue", item.getOldValue());
            vo.put("newValue", item.getNewValue());
            vo.put("auditStatus", item.getAuditStatus());
            vo.put("auditRemark", item.getAuditRemark());
            vo.put("auditTime", item.getAuditTime());
            vo.put("createTime", item.getCreateTime());

            // 补充企业名称
            EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(item.getEnterpriseId());
            if (enterprise != null) {
                vo.put("enterpriseName", enterprise.getEnterpriseName());
                vo.put("enterpriseType", enterprise.getEnterpriseType());
            }

            // 补充用户名称
            if (item.getUserId() != null) {
                SysUser user = sysUserMapper.selectById(item.getUserId());
                if (user != null) {
                    vo.put("username", user.getUsername());
                    vo.put("realName", user.getRealName());
                }
            }

            records.add(vo);
        }
        voPage.setRecords(records);
        return voPage;
    }

    /**
     * 管理员审核图片变更
     * @param auditId 审核记录ID
     * @param approved true=通过 false=拒绝
     * @param remark 审核备注（拒绝时为拒绝原因）
     * @param adminId 审核人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveAudit(Long auditId, boolean approved, String remark, Long adminId) {
        EnterpriseAuditImage audit = auditImageMapper.selectById(auditId);
        if (audit == null) {
            throw new RuntimeException("审核记录不存在");
        }
        if (audit.getAuditStatus() != 0) {
            throw new RuntimeException("该记录已审核，不可重复操作");
        }

        audit.setAuditStatus(approved ? 1 : 2);
        audit.setAuditRemark(remark);
        audit.setAuditBy(adminId);
        audit.setAuditTime(LocalDateTime.now());
        audit.setUpdateTime(LocalDateTime.now());
        auditImageMapper.updateById(audit);

        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(audit.getEnterpriseId());
        String enterpriseName = enterprise != null ? enterprise.getEnterpriseName() : "未知企业";
        String fieldLabel = fieldLabel(audit.getFieldName());

        if (approved) {
            // 通过：将新图片写入到企业表/用户表对应字段
            applyImageChange(audit);

            // 发送通知
            messageService.sendToEnterprise(audit.getEnterpriseId(), "system",
                    "图片审核通过",
                    "您提交的「" + fieldLabel + "」变更已审核通过",
                    "尊敬的用户，\n\n您提交的「" + fieldLabel + "」变更申请已审核通过，新图片已生效。",
                    null, null, null);

            log.info("图片审核通过, auditId={}, field={}, enterpriseId={}", auditId, audit.getFieldName(), audit.getEnterpriseId());
        } else {
            // 拒绝：发送通知含拒绝原因
            messageService.sendToEnterprise(audit.getEnterpriseId(), "system",
                    "图片审核未通过",
                    "您提交的「" + fieldLabel + "」变更未通过审核",
                    "尊敬的用户，\n\n您提交的「" + fieldLabel + "」变更申请未通过审核。\n\n拒绝原因：" + (remark != null ? remark : "无") + "\n\n请重新上传符合要求的图片。",
                    null, null, null);

            log.info("图片审核拒绝, auditId={}, field={}, reason={}", auditId, audit.getFieldName(), remark);
        }
    }

    /**
     * 查询企业的待审核图片数量
     */
    public long getPendingCount(Long enterpriseId) {
        LambdaQueryWrapper<EnterpriseAuditImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnterpriseAuditImage::getEnterpriseId, enterpriseId)
               .eq(EnterpriseAuditImage::getAuditStatus, 0);
        return auditImageMapper.selectCount(wrapper);
    }

    public long getQualificationPendingCount() {
        LambdaQueryWrapper<EnterpriseAuditImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnterpriseAuditImage::getAuditStatus, 0);
        return auditImageMapper.selectCount(wrapper);
    }

    /**
     * 查询企业的图片审核记录列表
     */
    public List<EnterpriseAuditImage> getByEnterprise(Long enterpriseId) {
        LambdaQueryWrapper<EnterpriseAuditImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnterpriseAuditImage::getEnterpriseId, enterpriseId)
               .orderByDesc(EnterpriseAuditImage::getCreateTime);
        return auditImageMapper.selectList(wrapper);
    }

    /**
     * 将审核通过的图片写入企业表/用户表
     */
    private void applyImageChange(EnterpriseAuditImage audit) {
        if ("avatar".equals(audit.getFieldName())) {
            // 头像写入sys_user表
            if (audit.getUserId() != null) {
                SysUser user = sysUserMapper.selectById(audit.getUserId());
                if (user != null) {
                    user.setAvatar(audit.getNewValue());
                    user.setUpdateTime(LocalDateTime.now());
                    sysUserMapper.updateById(user);
                }
            }
        } else {
            // 营业执照/许可证/logo写入enterprise_info表
            EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(audit.getEnterpriseId());
            if (enterprise != null) {
                switch (audit.getFieldName()) {
                    case "business_license":
                        enterprise.setBusinessLicense(audit.getNewValue());
                        break;
                    case "production_license":
                        enterprise.setProductionLicense(audit.getNewValue());
                        break;
                    case "logo":
                        enterprise.setLogo(audit.getNewValue());
                        break;
                    case "cover_image":
                        enterprise.setCoverImage(audit.getNewValue());
                        break;
                    default:
                        break;
                }
                enterprise.setUpdateTime(LocalDateTime.now());
                enterpriseInfoMapper.updateById(enterprise);
            }
        }
    }

    /**
     * 聚合所有业务表中的图片记录（分页）
     * 来源：运输/仓储/销售/加工/检疫
     */
    public Map<String, Object> getAllBusinessImages(long current, long size) {
        List<Map<String, Object>> all = new ArrayList<>();

        // 运输记录图片
        List<TraceTransport> transports = transportMapper.selectList(
                new LambdaQueryWrapper<TraceTransport>().isNotNull(TraceTransport::getImages).ne(TraceTransport::getImages, "").ne(TraceTransport::getImages, "[]"));
        for (TraceTransport t : transports) {
            List<String> urls = parseJsonArray(t.getImages());
            for (String url : urls) {
                Map<String, Object> item = new HashMap<>();
                item.put("sourceType", "transport");
                item.put("sourceLabel", "运输记录");
                item.put("sourceId", t.getId());
                item.put("batchCode", t.getBatchCode());
                item.put("imageUrl", url);
                item.put("createTime", t.getCreateTime());
                item.put("enterpriseId", t.getTransportEnterpriseId());
                item.put("enterpriseName", getEnterpriseName(t.getTransportEnterpriseId()));
                all.add(item);
            }
        }

        // 仓储记录图片
        List<TraceStorage> storages = storageMapper.selectList(
                new LambdaQueryWrapper<TraceStorage>().isNotNull(TraceStorage::getImages).ne(TraceStorage::getImages, "").ne(TraceStorage::getImages, "[]"));
        for (TraceStorage s : storages) {
            List<String> urls = parseJsonArray(s.getImages());
            for (String url : urls) {
                Map<String, Object> item = new HashMap<>();
                item.put("sourceType", "storage");
                item.put("sourceLabel", "仓储记录");
                item.put("sourceId", s.getId());
                item.put("batchCode", s.getBatchCode());
                item.put("imageUrl", url);
                item.put("createTime", s.getCreateTime());
                item.put("enterpriseId", s.getStorageEnterpriseId());
                item.put("enterpriseName", getEnterpriseName(s.getStorageEnterpriseId()));
                all.add(item);
            }
        }

        // 销售记录图片
        List<TraceSale> sales = saleMapper.selectList(
                new LambdaQueryWrapper<TraceSale>().isNotNull(TraceSale::getSaleVoucher).ne(TraceSale::getSaleVoucher, "").ne(TraceSale::getSaleVoucher, "[]"));
        for (TraceSale s : sales) {
            List<String> urls = parseJsonArray(s.getSaleVoucher());
            for (String url : urls) {
                Map<String, Object> item = new HashMap<>();
                item.put("sourceType", "sale");
                item.put("sourceLabel", "销售记录");
                item.put("sourceId", s.getId());
                item.put("batchCode", s.getBatchCode());
                item.put("imageUrl", url);
                item.put("createTime", s.getCreateTime());
                item.put("enterpriseId", s.getSaleEnterpriseId());
                item.put("enterpriseName", getEnterpriseName(s.getSaleEnterpriseId()));
                all.add(item);
            }
        }

        // 加工记录图片
        List<TraceProcessing> processings = processingMapper.selectList(
                new LambdaQueryWrapper<TraceProcessing>().isNotNull(TraceProcessing::getImages).ne(TraceProcessing::getImages, "").ne(TraceProcessing::getImages, "[]"));
        for (TraceProcessing p : processings) {
            List<String> urls = parseJsonArray(p.getImages());
            for (String url : urls) {
                Map<String, Object> item = new HashMap<>();
                item.put("sourceType", "processing");
                item.put("sourceLabel", "加工记录");
                item.put("sourceId", p.getId());
                item.put("batchCode", p.getSourceBatchCode());
                item.put("imageUrl", url);
                item.put("createTime", p.getCreateTime());
                item.put("enterpriseId", p.getProcessingEnterpriseId());
                item.put("enterpriseName", getEnterpriseName(p.getProcessingEnterpriseId()));
                all.add(item);
            }
        }

        // 检疫记录图片
        List<TraceInspection> inspections = inspectionMapper.selectList(
                new LambdaQueryWrapper<TraceInspection>().isNotNull(TraceInspection::getCertImage).ne(TraceInspection::getCertImage, "").ne(TraceInspection::getCertImage, "[]"));
        for (TraceInspection i : inspections) {
            List<String> urls = parseJsonArray(i.getCertImage());
            for (String url : urls) {
                Map<String, Object> item = new HashMap<>();
                item.put("sourceType", "inspection");
                item.put("sourceLabel", "检疫记录");
                item.put("sourceId", i.getId());
                item.put("batchCode", i.getBatchCode());
                item.put("imageUrl", url);
                item.put("createTime", i.getCreateTime());
                item.put("enterpriseId", i.getInspectionEnterpriseId());
                item.put("enterpriseName", getEnterpriseName(i.getInspectionEnterpriseId()));
                all.add(item);
            }
        }

        // 按时间倒序
        all.sort((a, b) -> {
            Object ta = a.get("createTime");
            Object tb = b.get("createTime");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.toString().compareTo(ta.toString());
        });

        // 手动分页
        long total = all.size();
        int fromIndex = (int) ((current - 1) * size);
        int toIndex = (int) Math.min(fromIndex + size, total);
        List<Map<String, Object>> pageRecords = fromIndex >= total ? new ArrayList<>() : all.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageRecords);
        result.put("total", total);
        result.put("current", current);
        result.put("size", size);
        return result;
    }

    private String getEnterpriseName(Long enterpriseId) {
        if (enterpriseId == null) return "";
        EnterpriseInfo e = enterpriseInfoMapper.selectById(enterpriseId);
        return e != null ? e.getEnterpriseName() : "";
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // 可能是单个URL字符串
            if (json.startsWith("http") || json.startsWith("/")) {
                return Collections.singletonList(json);
            }
            return Collections.emptyList();
        }
    }

    /**
     * 字段名转中文标签
     */
    private static String fieldLabel(String fieldName) {
        if (fieldName == null) return "未知";
        switch (fieldName) {
            case "avatar": return "用户头像";
            case "business_license": return "营业执照";
            case "production_license": return "行业许可证";
            case "logo": return "企业Logo";
            case "cover_image": return "企业背景图";
            default: return fieldName;
        }
    }

    /**
     * 业务图片违规处理：从对应业务记录的images字段中移除该图片URL，并发送通知
     */
    @Transactional(rollbackFor = Exception.class)
    public void markBusinessImageViolation(String sourceType, Long sourceId, String imageUrl) {
        Long enterpriseId = removeImageFromSource(sourceType, sourceId, imageUrl);
        if (enterpriseId != null) {
            messageService.sendToEnterprise(enterpriseId, "system",
                    "图片违规通知",
                    "您上传的图片因违规已被移除",
                    "您在" + sourceTypeLabel(sourceType) + "中上传的图片因违规已被管理员移除，请上传符合规范的图片。",
                    null, null, null);
        }
    }

    /**
     * 业务图片删除：从对应业务记录的images字段中移除该图片URL，不发送通知
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBusinessImage(String sourceType, Long sourceId, String imageUrl) {
        removeImageFromSource(sourceType, sourceId, imageUrl);
    }

    /**
     * 从业务记录中移除指定图片URL，返回企业ID
     */
    private Long removeImageFromSource(String sourceType, Long sourceId, String imageUrl) {
        if (sourceType == null || sourceId == null || imageUrl == null) return null;
        try {
            switch (sourceType) {
                case "transport": {
                    TraceTransport r = transportMapper.selectById(sourceId);
                    if (r == null) return null;
                    r.setImages(removeUrlFromJson(r.getImages(), imageUrl));
                    r.setUpdateTime(LocalDateTime.now());
                    transportMapper.updateById(r);
                    return r.getTransportEnterpriseId();
                }
                case "storage": {
                    TraceStorage r = storageMapper.selectById(sourceId);
                    if (r == null) return null;
                    r.setImages(removeUrlFromJson(r.getImages(), imageUrl));
                    r.setUpdateTime(LocalDateTime.now());
                    storageMapper.updateById(r);
                    return r.getStorageEnterpriseId();
                }
                case "sale": {
                    TraceSale r = saleMapper.selectById(sourceId);
                    if (r == null) return null;
                    r.setSaleVoucher(removeUrlFromJson(r.getSaleVoucher(), imageUrl));
                    r.setUpdateTime(LocalDateTime.now());
                    saleMapper.updateById(r);
                    return r.getSaleEnterpriseId();
                }
                case "processing": {
                    TraceProcessing r = processingMapper.selectById(sourceId);
                    if (r == null) return null;
                    r.setImages(removeUrlFromJson(r.getImages(), imageUrl));
                    r.setUpdateTime(LocalDateTime.now());
                    processingMapper.updateById(r);
                    return r.getProcessingEnterpriseId();
                }
                case "inspection": {
                    TraceInspection r = inspectionMapper.selectById(sourceId);
                    if (r == null) return null;
                    r.setCertImage(removeUrlFromJson(r.getCertImage(), imageUrl));
                    r.setUpdateTime(LocalDateTime.now());
                    inspectionMapper.updateById(r);
                    return r.getInspectionEnterpriseId();
                }
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("removeImageFromSource error: sourceType={}, sourceId={}", sourceType, sourceId, e);
            return null;
        }
    }

    private String removeUrlFromJson(String json, String urlToRemove) {
        List<String> urls = parseJsonArray(json);
        urls = new ArrayList<>(urls);
        urls.remove(urlToRemove);
        try {
            return JSON.writeValueAsString(urls);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String sourceTypeLabel(String sourceType) {
        if (sourceType == null) return "业务记录";
        switch (sourceType) {
            case "transport": return "运输记录";
            case "storage": return "仓储记录";
            case "sale": return "销售记录";
            case "processing": return "加工记录";
            case "inspection": return "检疫记录";
            default: return "业务记录";
        }
    }

    /**
     * 资质图片违规处理：标记审核记录为违规(status=3)，清除已生效的图片，发送通知
     */
    @Transactional(rollbackFor = Exception.class)
    public void markQualificationViolation(Long auditId) {
        EnterpriseAuditImage audit = auditImageMapper.selectById(auditId);
        if (audit == null) throw new RuntimeException("审核记录不存在");
        audit.setAuditStatus(3);
        audit.setAuditRemark("图片违规");
        audit.setUpdateTime(LocalDateTime.now());
        auditImageMapper.updateById(audit);

        // 如果之前已通过并生效，需要回退
        if (audit.getOldValue() != null) {
            revertImageChange(audit);
        }

        messageService.sendToEnterprise(audit.getEnterpriseId(), "system",
                "图片违规通知",
                "您提交的「" + fieldLabel(audit.getFieldName()) + "」图片违规，请重新上传",
                "您提交的「" + fieldLabel(audit.getFieldName()) + "」图片因违规已被管理员标记，请重新上传符合规范的图片。",
                null, null, null);
    }

    /**
     * 删除资质图片审核记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteQualificationAudit(Long auditId) {
        EnterpriseAuditImage audit = auditImageMapper.selectById(auditId);
        if (audit == null) throw new RuntimeException("审核记录不存在");
        auditImageMapper.deleteById(auditId);
    }

    /**
     * 回退已生效的图片变更（恢复旧值）
     */
    private void revertImageChange(EnterpriseAuditImage audit) {
        if ("avatar".equals(audit.getFieldName())) {
            if (audit.getUserId() != null) {
                SysUser user = sysUserMapper.selectById(audit.getUserId());
                if (user != null && audit.getOldValue() != null) {
                    user.setAvatar(audit.getOldValue());
                    user.setUpdateTime(LocalDateTime.now());
                    sysUserMapper.updateById(user);
                }
            }
        } else {
            EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(audit.getEnterpriseId());
            if (enterprise != null && audit.getOldValue() != null) {
                switch (audit.getFieldName()) {
                    case "business_license": enterprise.setBusinessLicense(audit.getOldValue()); break;
                    case "production_license": enterprise.setProductionLicense(audit.getOldValue()); break;
                    case "logo": enterprise.setLogo(audit.getOldValue()); break;
                    case "cover_image": enterprise.setCoverImage(audit.getOldValue()); break;
                    default: break;
                }
                enterprise.setUpdateTime(LocalDateTime.now());
                enterpriseInfoMapper.updateById(enterprise);
            }
        }
    }
}
