package com.agricultural.trace.service;

import com.agricultural.trace.dto.QuarantineSubmitDTO;
import com.agricultural.trace.entity.EnterpriseInfo;
import com.agricultural.trace.entity.SysUser;
import com.agricultural.trace.entity.TraceBatch;
import com.agricultural.trace.entity.TraceInspection;
import com.agricultural.trace.mapper.EnterpriseInfoMapper;
import com.agricultural.trace.mapper.SysUserMapper;
import com.agricultural.trace.mapper.TraceBatchMapper;
import com.agricultural.trace.mapper.TraceInspectionMapper;
import com.agricultural.trace.utils.Web3jUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 检疫服务
 * 处理检疫结果提交、数据哈希计算、区块链上链、批次状态更新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuarantineService {

    private final TraceBatchMapper traceBatchMapper;
    private final TraceInspectionMapper traceInspectionMapper;
    private final SysUserMapper sysUserMapper;
    private final Web3jUtils web3jUtils;
    private final BlockchainTransactionService blockchainTransactionService;
    private final MessageService messageService;
    private final EnterpriseInfoMapper enterpriseInfoMapper;

    /**
     * 查询企业的检疫申报列表
     * 通过查询 trace_inspection 表中 batch 所属企业的记录来展示
     * check_result 为 null 表示待受理，有值表示已完成
     *
     * @param enterpriseId 企业ID
     * @param batchCode    批次号模糊搜索（可选）
     * @param status       pending=待受理(check_result IS NULL), completed=已完成(check_result IS NOT NULL)
     * @return 检疫记录列表
     */
    public List<Map<String, Object>> listApplyByEnterprise(Long enterpriseId, String batchCode, String status) {
        // 1. Query batches, optionally filtered by enterprise
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TraceBatch> batchWrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            batchWrapper.eq(TraceBatch::getEnterpriseId, enterpriseId);
        }
        if (batchCode != null && !batchCode.trim().isEmpty()) {
            batchWrapper.like(TraceBatch::getBatchCode, batchCode.trim());
        }
        List<TraceBatch> batches = traceBatchMapper.selectList(batchWrapper);
        if (batches.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 构建批次ID -> 批次信息的映射
        Map<Long, TraceBatch> batchMap = new HashMap<>();
        List<Long> batchIds = new ArrayList<>();
        for (TraceBatch b : batches) {
            batchMap.put(b.getId(), b);
            batchIds.add(b.getId());
        }

        // 3. 查询这些批次的检疫记录
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TraceInspection> inspWrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        inspWrapper.in(TraceInspection::getBatchId, batchIds);

        // 状态过滤
        if ("pending".equals(status)) {
            inspWrapper.isNull(TraceInspection::getCheckResult);
        } else if ("completed".equals(status)) {
            inspWrapper.isNotNull(TraceInspection::getCheckResult);
        }

        inspWrapper.orderByDesc(TraceInspection::getCreateTime);
        List<TraceInspection> inspections = traceInspectionMapper.selectList(inspWrapper);

        // 4. 组装返回数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (TraceInspection insp : inspections) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", insp.getId());
            item.put("batchId", insp.getBatchId());
            item.put("batchCode", insp.getBatchCode());
            item.put("inspectionDate", insp.getInspectionDate() != null ? insp.getInspectionDate().toString() : null);
            item.put("checkResult", insp.getCheckResult());
            // null=待受理, 1=合格, 0=不合格
            String statusText;
            if (insp.getCheckResult() == null) {
                statusText = "待受理";
            } else if (insp.getCheckResult() == 1) {
                statusText = "合格";
            } else {
                statusText = "不合格";
            }
            item.put("statusText", statusText);
            item.put("certNo", insp.getCertNo());
            item.put("inspector", insp.getInspector());
            item.put("txHash", insp.getTxHash());
            item.put("remark", insp.getRemark());
            item.put("createTime", insp.getCreateTime() != null ? insp.getCreateTime().toString() : null);

            // 补充批次信息
            TraceBatch batch = batchMap.get(insp.getBatchId());
            if (batch != null) {
                item.put("productName", batch.getProductName());
                item.put("quantity", batch.getCurrentQuantity() != null ? batch.getCurrentQuantity() : batch.getInitQuantity());
                item.put("unit", batch.getUnit());
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 检疫机构查询所有待处理的检疫申报列表
     * 查询所有 inspection_enterprise_id 匹配的检疫记录
     *
     * @param inspectionEnterpriseId 检疫机构ID（可选，不传则查询所有）
     * @param batchCode              批次号模糊搜索（可选）
     * @param status                 pending=待受理, completed=已完成（可选）
     * @return 检疫记录列表
     */
    public List<Map<String, Object>> listApplyForInspector(Long inspectionEnterpriseId, String batchCode, String status) {
        log.info("listApplyForInspector called with inspectionEnterpriseId={}, batchCode={}, status={}", 
                inspectionEnterpriseId, batchCode, status);
        
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TraceInspection> inspWrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        if (inspectionEnterpriseId != null) {
            inspWrapper.eq(TraceInspection::getInspectionEnterpriseId, inspectionEnterpriseId);
        }
        
        if (batchCode != null && !batchCode.trim().isEmpty()) {
            inspWrapper.like(TraceInspection::getBatchCode, batchCode.trim());
        }
        
        if ("pending".equals(status)) {
            inspWrapper.isNull(TraceInspection::getCheckResult);
        } else if ("completed".equals(status)) {
            inspWrapper.isNotNull(TraceInspection::getCheckResult);
        }
        
        inspWrapper.orderByDesc(TraceInspection::getCreateTime);
        List<TraceInspection> inspections = traceInspectionMapper.selectList(inspWrapper);
        
        log.info("Found {} inspection records", inspections.size());
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (TraceInspection insp : inspections) {
            TraceBatch batch = traceBatchMapper.selectById(insp.getBatchId());
            
            Map<String, Object> item = new HashMap<>();
            item.put("id", insp.getId());
            item.put("batchId", insp.getBatchId());
            item.put("batchCode", insp.getBatchCode());
            item.put("inspectionDate", insp.getInspectionDate() != null ? insp.getInspectionDate().toString() : null);
            item.put("checkResult", insp.getCheckResult());
            
            String statusText;
            if (insp.getCheckResult() == null) {
                statusText = "待检疫";
            } else if (insp.getCheckResult() == 1) {
                statusText = "合格";
            } else {
                statusText = "不合格";
            }
            item.put("statusText", statusText);
            item.put("certNo", insp.getCertNo());
            item.put("inspector", insp.getInspector());
            item.put("txHash", insp.getTxHash());
            item.put("remark", insp.getRemark());
            item.put("createTime", insp.getCreateTime() != null ? insp.getCreateTime().toString() : null);
            item.put("applyTime", insp.getCreateTime() != null ? insp.getCreateTime().toString() : null);
            item.put("images", insp.getImages());
            
            if (batch != null) {
                item.put("productName", batch.getProductName());
                item.put("quantity", batch.getCurrentQuantity() != null ? batch.getCurrentQuantity() : batch.getInitQuantity());
                item.put("unit", batch.getUnit());
                item.put("productType", batch.getProductType());
                
                String productTypeTag = "未知";
                if (batch.getProductType() != null) {
                    switch (batch.getProductType()) {
                        case 1: productTypeTag = "肉鸡"; break;
                        case 2: productTypeTag = "蔬菜"; break;
                        case 3: productTypeTag = "水果"; break;
                        default: productTypeTag = "其他";
                    }
                }
                item.put("productTypeTag", productTypeTag);
                
                if (batch.getEnterpriseId() != null) {
                    EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(batch.getEnterpriseId());
                    if (enterprise != null) {
                        item.put("enterpriseName", enterprise.getEnterpriseName());
                    }
                }
            }
            
            result.add(item);
        }
        
        log.info("Returning {} formatted records", result.size());
        return result;
    }

    /**
     * 删除检疫记录
     * 如果是已完成检疫的记录（checkResult不为空）：清空检疫结果，批次状态恢复为待检疫（状态3）
     * 如果是待检疫的记录（checkResult为空）：直接删除该检疫申报记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteApply(Long id) {
        log.info("开始删除检疫记录，id={}", id);
        
        TraceInspection inspection = traceInspectionMapper.selectById(id);
        if (inspection == null) {
            throw new RuntimeException("检疫申报记录不存在");
        }
        
        log.info("检疫记录状态：id={}, batchId={}, checkResult={}", 
                inspection.getId(), inspection.getBatchId(), inspection.getCheckResult());
        
        if (inspection.getCheckResult() == null) {
            log.info("待检疫记录，执行物理删除");
            traceInspectionMapper.deleteById(id);
            log.info("检疫记录已删除");
        } else {
            log.info("已完成检疫记录，执行回滚操作");
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TraceInspection> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
            updateWrapper.eq("id", id)
                        .set("check_result", null)
                        .set("inspector", null)
                        .set("inspector_code", null)
                        .set("inspector_id", null)
                        .set("cert_no", null)
                        .set("cert_image", null)
                        .set("inspection_items", null)
                        .set("update_time", LocalDateTime.now());
            
            int updateCount = traceInspectionMapper.update(null, updateWrapper);
            log.info("检疫记录回滚影响行数：{}", updateCount);

            TraceBatch batch = traceBatchMapper.selectById(inspection.getBatchId());
            if (batch != null) {
                log.info("批次 {} 回滚前状态：{}", batch.getBatchCode(), batch.getBatchStatus());
                batch.setBatchStatus(3);
                batch.setUpdateTime(LocalDateTime.now());
                int batchUpdateCount = traceBatchMapper.updateById(batch);
                log.info("批次更新影响行数：{}", batchUpdateCount);
                
                TraceBatch updatedBatch = traceBatchMapper.selectById(batch.getId());
                log.info("批次 {} 回滚后状态：{}", updatedBatch.getBatchCode(), updatedBatch.getBatchStatus());
            }
        }
    }

    /**
     * Farmer applies for quarantine inspection.
     * Creates a pending inspection record, updates batch status to 3 (pending quarantine).
     * Does NOT fill checkResult/certNo/inspector, does NOT upload certificate to blockchain.
     * Only uploads the "apply" action hash to blockchain.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyQuarantine(Map<String, Object> params) {
        // 1. Resolve batch
        String batchCode = params.get("batchCode") != null ? params.get("batchCode").toString().trim() : null;
        Long batchId = params.get("batchId") != null ? Long.parseLong(params.get("batchId").toString()) : null;

        TraceBatch batch = null;
        if (batchCode != null && !batchCode.isEmpty()) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TraceBatch> w =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            w.eq(TraceBatch::getBatchCode, batchCode);
            batch = traceBatchMapper.selectOne(w);
        } else if (batchId != null) {
            batch = traceBatchMapper.selectById(batchId);
        }
        if (batch == null) {
            throw new RuntimeException("batch not found");
        }

        // 2. Create a pending inspection record (no result yet)
        TraceInspection inspection = new TraceInspection();
        inspection.setBatchId(batch.getId());
        inspection.setBatchCode(batch.getBatchCode());

        // Expected inspection date
        String expectedDate = params.get("expectedDate") != null ? params.get("expectedDate").toString().trim() : null;
        if (expectedDate != null && !expectedDate.isEmpty()) {
            inspection.setInspectionDate(LocalDate.parse(expectedDate));
        } else {
            inspection.setInspectionDate(LocalDate.now());
        }

        // Apply quantity
        // checkResult = null means pending
        inspection.setCheckResult(null);
        inspection.setRemark(params.get("remark") != null ? params.get("remark").toString() : null);

        // 检疫机构ID：优先使用传入的inspectionEnterpriseId，否则查找默认检疫机构（type=3）
        Long inspectionEnterpriseId = params.get("inspectionEnterpriseId") != null 
            ? Long.parseLong(params.get("inspectionEnterpriseId").toString()) 
            : null;
        
        if (inspectionEnterpriseId == null) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EnterpriseInfo> entWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            entWrapper.eq(EnterpriseInfo::getEnterpriseType, 3)
                     .eq(EnterpriseInfo::getStatus, 1)
                     .eq(EnterpriseInfo::getAuditStatus, 1)
                     .last("LIMIT 1");
            EnterpriseInfo defaultInspectionEnt = enterpriseInfoMapper.selectOne(entWrapper);
            if (defaultInspectionEnt != null) {
                inspectionEnterpriseId = defaultInspectionEnt.getId();
            }
        }
        
        inspection.setInspectionEnterpriseId(inspectionEnterpriseId);

        // 检疫记录需独立上链，不继承批次的上链信息

        // 记录申报人 ID
        Long applicantUserId = params.get("userId") != null ? Long.parseLong(params.get("userId").toString()) : null;
        inspection.setCreateBy(applicantUserId);

        inspection.setCreateTime(LocalDateTime.now());
        inspection.setUpdateTime(LocalDateTime.now());
        traceInspectionMapper.insert(inspection);

        // 3. Update batch status to 3 (pending quarantine)
        batch.setBatchStatus(3);
        batch.setUpdateTime(LocalDateTime.now());
        traceBatchMapper.updateById(batch);
        log.info("batch {} status updated to 3 (pending quarantine), inspectionEnterpriseId={}", 
                batch.getBatchCode(), inspectionEnterpriseId);

        // 4. Return result (上链统一在仓储记录提交时执行)
        Map<String, Object> result = new HashMap<>();
        result.put("inspectionId", inspection.getId());
        result.put("batchId", batch.getId());
        result.put("batchCode", batch.getBatchCode());
        result.put("batchStatus", 3);
        result.put("inspectionEnterpriseId", inspectionEnterpriseId);
        result.put("txHash", null);
        return result;
    }

    /**
     * 提交检疫结果并上链存证
     *
     * 业务流程：
     * 1. 校验批次和检疫员信息
     * 2. 查找该批次是否已有待检疫记录，如果有则更新，否则新增
     * 3. 检疫合格时，更新 trace_batch 状态为 5（已检疫）
     * 4. 发送业务提醒给申报人
     *
     * @param dto 检疫提交数据
     * @return 包含检疫结果的信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitQuarantine(QuarantineSubmitDTO dto) {

        // ========== 1. 参数校验 + batchCode -> batchId 解析 ==========
        TraceBatch batch = null;
        if (dto.getBatchId() == null && dto.getBatchCode() != null && !dto.getBatchCode().trim().isEmpty()) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TraceBatch> batchWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            batchWrapper.eq(TraceBatch::getBatchCode, dto.getBatchCode().trim());
            batch = traceBatchMapper.selectOne(batchWrapper);
            if (batch == null) {
                throw new RuntimeException("batch not found for batchCode: " + dto.getBatchCode());
            }
            dto.setBatchId(batch.getId());
        } else if (dto.getBatchId() != null) {
            batch = traceBatchMapper.selectById(dto.getBatchId());
        }
        if (batch == null) {
            throw new RuntimeException("batch not found (provide batchCode or batchId)");
        }

        SysUser inspector = null;
        String inspectorName = dto.getInspector();
        if (dto.getInspectorId() != null) {
            inspector = sysUserMapper.selectById(dto.getInspectorId());
            if (inspector != null) {
                inspectorName = inspector.getRealName() != null ? inspector.getRealName() : inspector.getUsername();
            }
        }
        if (inspectorName == null || inspectorName.trim().isEmpty()) {
            inspectorName = "system";
        }

        log.info("开始提交检疫结果，批次: {}, 检疫员: {}, 结果: {}",
                batch.getBatchCode(), inspectorName, dto.getCheckResult());

        // ========== 2. 查找是否已有待检疫记录，如果有则更新，否则新增 ==========
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TraceInspection> queryWrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        queryWrapper.eq(TraceInspection::getBatchId, dto.getBatchId())
                   .isNull(TraceInspection::getCheckResult)
                   .orderByDesc(TraceInspection::getCreateTime)
                   .last("LIMIT 1");
        TraceInspection inspection = traceInspectionMapper.selectOne(queryWrapper);

        boolean isUpdate = false;
        if (inspection != null) {
            isUpdate = true;
            log.info("找到待检疫记录 id={}, 将更新该记录", inspection.getId());
            // 确保 inspectionEnterpriseId 被正确设置（修复旧数据或申报时未设置的情况）
            if (dto.getEnterpriseId() != null && (inspection.getInspectionEnterpriseId() == null
                    || !dto.getEnterpriseId().equals(inspection.getInspectionEnterpriseId()))) {
                inspection.setInspectionEnterpriseId(dto.getEnterpriseId());
                log.info("更新检疫记录的 inspectionEnterpriseId={}", dto.getEnterpriseId());
            }
        } else {
            inspection = new TraceInspection();
            inspection.setBatchId(dto.getBatchId());
            inspection.setBatchCode(batch.getBatchCode());
            inspection.setInspectionEnterpriseId(dto.getEnterpriseId());
            inspection.setCreateTime(LocalDateTime.now());
            inspection.setCreateBy(dto.getInspectorId());
            log.info("未找到待检疫记录，将新增记录");
        }

        if (dto.getInspectionDate() != null && !dto.getInspectionDate().trim().isEmpty()) {
            inspection.setInspectionDate(LocalDate.parse(dto.getInspectionDate().trim()));
        } else {
            inspection.setInspectionDate(LocalDate.now());
        }
        inspection.setInspectionType(dto.getInspectionType() != null ? dto.getInspectionType() : 1);
        inspection.setInspector(inspectorName);
        inspection.setInspectorCode(dto.getInspectorCode());
        inspection.setInspectionCertificateNo(dto.getEffectiveCertificateNo());
        inspection.setInspectionResult(dto.getCheckResult());
        inspection.setInspectionItems(dto.getInspectionItems());
        
        String imageData = dto.getImagePath();
        if ((imageData == null || imageData.trim().isEmpty()) && dto.getCertImage() != null) {
            imageData = dto.getCertImage();
        }
        inspection.setImages(imageData);
        inspection.setUnqualifiedReason(dto.getUnqualifiedReason());
        inspection.setRemark(dto.getRemark());
        inspection.setUpdateBy(dto.getInspectorId());
        inspection.setUpdateTime(LocalDateTime.now());

        if (isUpdate) {
            traceInspectionMapper.updateById(inspection);
            log.info("检疫记录已更新，inspection.id: {}", inspection.getId());
        } else {
            traceInspectionMapper.insert(inspection);
            log.info("检疫记录已新增，inspection.id: {}", inspection.getId());
        }

        // ====== PC端带图片提交时触发上链（imagePath不为空说明是完整提交，且未上链过） ======
        String imagePath = dto.getImagePath();
        if (imagePath != null && !imagePath.trim().isEmpty() && dto.getCheckResult() != null && dto.getCheckResult() == 1
                && (inspection.getTxHash() == null || inspection.getTxHash().isEmpty())) {
            performChainUpload(inspection, batch);
        }

        // ========== 3. 检疫合格时更新批次状态为 5（已检疫） ==========
        if (dto.getCheckResult() != null && dto.getCheckResult() == 1) {
            batch.setBatchStatus(5);
            batch.setUpdateTime(LocalDateTime.now());
            batch.setUpdateBy(dto.getInspectorId());
            traceBatchMapper.updateById(batch);
            log.info("批次 {} 状态已更新为'已检疫'(5)", batch.getBatchCode());
        } else {
            log.info("检疫结果为不合格，批次 {} 状态不变", batch.getBatchCode());
        }

        // ========== 4. 发送业务提醒给申报员工 ==========
        try {
            // 查找该批次的原始申报记录（create_by 记录了申报人）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TraceInspection> applyWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            applyWrapper.eq(TraceInspection::getBatchId, batch.getId())
                    .isNotNull(TraceInspection::getCreateBy)
                    .orderByAsc(TraceInspection::getCreateTime)
                    .last("LIMIT 1");
            TraceInspection applyRecord = traceInspectionMapper.selectOne(applyWrapper);

            Long applicantUserId = null;
            if (applyRecord != null && applyRecord.getCreateBy() != null) {
                applicantUserId = applyRecord.getCreateBy();
            }

            if (applicantUserId != null) {
                // 检疫企业名称: 优先从inspection记录获取，回退到dto
                String inspEntName = "";
                Long inspEntId = inspection.getInspectionEnterpriseId();
                if (inspEntId == null) {
                    inspEntId = dto.getEnterpriseId();
                }
                if (inspEntId != null) {
                    EnterpriseInfo inspEnt = enterpriseInfoMapper.selectById(inspEntId);
                    if (inspEnt != null) {
                        inspEntName = inspEnt.getEnterpriseName();
                    }
                }
                String resultText = (dto.getCheckResult() != null && dto.getCheckResult() == 1) ? "合格" : "不合格";
                String msgTitle = "【检疫审核】您的检疫申报已审核";
                String msgSummary = "批次 " + batch.getBatchCode() + " 的检疫申报已由 " + inspEntName + " 审核，结果：" + resultText;
                String msgContent = "检疫审核结果通知\n\n"
                        + "批次编号：" + batch.getBatchCode() + "\n"
                        + "产品名称：" + batch.getProductName() + "\n"
                        + "检疫机构：" + inspEntName + "\n"
                        + "检疫员：" + inspectorName + "\n"
                        + "检疫日期：" + inspection.getInspectionDate() + "\n"
                        + "审核结果：" + resultText + "\n"
                        + (dto.getCheckResult() != null && dto.getCheckResult() == 0 && dto.getUnqualifiedReason() != null
                            ? "不合格原因：" + dto.getUnqualifiedReason() + "\n" : "")
                        + (dto.getEffectiveCertificateNo() != null ? "证书编号：" + dto.getEffectiveCertificateNo() + "\n" : "");
                messageService.sendMessage(applicantUserId, "business", msgTitle, msgSummary, msgContent,
                        null, null, null);
                log.info("检疫审核业务提醒已发送给用户 userId={}", applicantUserId);
            }
        } catch (Exception msgEx) {
            log.warn("发送检疫审核通知失败(不影响业务): {}", msgEx.getMessage());
        }

        // ========== 5. 组装返回结果 ==========
        Map<String, Object> result = new HashMap<>();
        result.put("inspectionId", inspection.getId());
        result.put("batchId", batch.getId());
        result.put("batchCode", batch.getBatchCode());
        result.put("checkResult", dto.getCheckResult());
        result.put("checkResultText", dto.getCheckResult() == 1 ? "合格" : "不合格");
        result.put("txHash", inspection.getTxHash());
        result.put("dataHash", inspection.getDataHash());
        result.put("blockNumber", inspection.getBlockNumber());
        result.put("chainTime", inspection.getChainTime() != null ? inspection.getChainTime().toString() : null);
        result.put("batchStatus", batch.getBatchStatus());

        return result;
    }

    /**
     * 更新检疫证书信息
     * 仅更新证书图片、证书编号和备注，不修改检疫结果
     *
     * @param id 检疫记录ID
     * @param certImage 证书图片路径
     * @param certNo 证书编号
     * @param remark 备注
     * @return 更新结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateCertificate(Long id, String certImage, String certNo, String remark) {
        log.info("更新检疫证书信息，id: {}, certNo: {}", id, certNo);
        
        TraceInspection inspection = traceInspectionMapper.selectById(id);
        if (inspection == null) {
            throw new RuntimeException("检疫记录不存在");
        }
        
        if (inspection.getCheckResult() == null) {
            throw new RuntimeException("该批次尚未完成检疫录入，请先进行检疫结果录入");
        }
        
        if (certImage != null && !certImage.trim().isEmpty()) {
            inspection.setImages(certImage);
        }
        
        if (certNo != null && !certNo.trim().isEmpty()) {
            inspection.setInspectionCertificateNo(certNo);
        }
        
        if (remark != null) {
            inspection.setRemark(remark);
        }
        
        inspection.setUpdateTime(LocalDateTime.now());
        traceInspectionMapper.updateById(inspection);
        
        log.info("检疫证书信息更新成功，inspection.id: {}", inspection.getId());

        // ====== 报告上传后触发上链（检疫合格且有图片，且未上链过，避免重复上链） ======
        TraceBatch batch = traceBatchMapper.selectById(inspection.getBatchId());
        if (batch != null && inspection.getCheckResult() != null && inspection.getCheckResult() == 1
                && inspection.getImages() != null && !inspection.getImages().trim().isEmpty()
                && (inspection.getTxHash() == null || inspection.getTxHash().isEmpty())) {
            performChainUpload(inspection, batch);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("inspectionId", inspection.getId());
        result.put("batchId", inspection.getBatchId());
        result.put("batchCode", inspection.getBatchCode());
        result.put("certNo", inspection.getInspectionCertificateNo());
        result.put("images", inspection.getImages());
        result.put("txHash", inspection.getTxHash());
        result.put("dataHash", inspection.getDataHash());
        result.put("blockNumber", inspection.getBlockNumber());
        result.put("chainTime", inspection.getChainTime() != null ? inspection.getChainTime().toString() : null);
        
        return result;
    }

    /**
     * 执行检疫上链操作
     * 打包批次信息 + 检疫结果生成哈希，调用区块链合约上传
     */
    private void performChainUpload(TraceInspection inspection, TraceBatch batch) {
        try {
            String dataHash = buildInspectionChainHash(batch, inspection);
            String traceId = batch.getBatchCode() + "_INSPECTION_" + inspection.getId();
            String txHash = web3jUtils.uploadHash(traceId, dataHash);

            Long blockNumber = null;
            try {
                org.web3j.protocol.core.methods.response.TransactionReceipt receipt =
                        web3jUtils.getTransactionReceipt(txHash);
                if (receipt != null) {
                    blockNumber = receipt.getBlockNumber().longValue();
                }
            } catch (Exception ignored) {}

            LocalDateTime chainTime = LocalDateTime.now();

            inspection.setTxHash(txHash);
            inspection.setDataHash(dataHash);
            inspection.setBlockNumber(blockNumber);
            inspection.setChainTime(chainTime);
            inspection.setUpdateTime(LocalDateTime.now());
            traceInspectionMapper.updateById(inspection);

            log.info("检疫记录上链成功, batchCode={}, txHash={}, blockNumber={}",
                    batch.getBatchCode(), txHash, blockNumber);

            // 记录交易到 blockchain_transaction 表
            org.web3j.protocol.core.methods.response.TransactionReceipt fullReceipt =
                    web3jUtils.getTransactionReceipt(txHash);
            blockchainTransactionService.recordTransaction(txHash,
                    BlockchainTransactionService.BIZ_INSPECTION, inspection.getId(),
                    batch.getId(), dataHash, fullReceipt);

            // 发送上链成功通知
            try {
                String inspEntName = "";
                if (inspection.getInspectionEnterpriseId() != null) {
                    EnterpriseInfo ent = enterpriseInfoMapper.selectById(inspection.getInspectionEnterpriseId());
                    if (ent != null) inspEntName = ent.getEnterpriseName();
                }
                String msgTitle = "【上链成功】检疫存证已上链";
                String msgSummary = "批次 " + batch.getBatchCode() + " 的检疫结果已成功上链";
                String msgContent = "上链成功通知\n\n"
                        + "检疫机构：" + inspEntName + "\n"
                        + "检疫员：" + inspection.getInspector() + "\n"
                        + "批次编号：" + batch.getBatchCode() + "\n"
                        + "产品名称：" + batch.getProductName() + "\n"
                        + "检疫结果：合格\n"
                        + "交易哈希：" + txHash + "\n"
                        + "区块高度：" + blockNumber + "\n"
                        + "上链时间：" + chainTime;
                if (inspection.getInspectionEnterpriseId() != null) {
                    messageService.sendToEnterprise(inspection.getInspectionEnterpriseId(), "system",
                            msgTitle, msgSummary, msgContent, null, null, null);
                }
            } catch (Exception msgEx) {
                log.warn("发送检疫上链通知失败(不影响业务): {}", msgEx.getMessage());
            }
        } catch (Exception e) {
            log.warn("检疫记录上链失败(不影响业务): {}", e.getMessage());
        }
    }

    /**
     * 构建检疫上链哈希
     * 包含：批次信息 + 检疫结果 + 证书信息
     */
    private String buildInspectionChainHash(TraceBatch batch, TraceInspection inspection) {
        StringBuilder sb = new StringBuilder();
        sb.append("BATCH|");
        sb.append("id=").append(batch.getId()).append("|");
        sb.append("code=").append(batch.getBatchCode()).append("|");
        sb.append("product=").append(batch.getProductName()).append("|");
        sb.append("enterprise=").append(batch.getEnterpriseId()).append("|");
        sb.append("INSPECTION|");
        sb.append("id=").append(inspection.getId()).append("|");
        sb.append("date=").append(inspection.getInspectionDate()).append("|");
        sb.append("result=").append(inspection.getCheckResult()).append("|");
        sb.append("certNo=").append(inspection.getCertNo() != null ? inspection.getCertNo() : "").append("|");
        sb.append("inspector=").append(inspection.getInspector() != null ? inspection.getInspector() : "").append("|");
        sb.append("items=").append(inspection.getInspectionItems() != null ? inspection.getInspectionItems() : "").append("|");
        sb.append("images=").append(inspection.getImages() != null ? inspection.getImages() : "").append("|");
        sb.append("ts=").append(System.currentTimeMillis());
        return computeSHA256(sb.toString());
    }

    /**
     * 计算 SHA-256 哈希值
     *
     * @param data 原始数据字符串
     * @return 64位十六进制哈希字符串（带 0x 前缀）
     */
    private String computeSHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("SHA-256计算失败", e);
            return "hash_error_" + System.currentTimeMillis();
        }
    }
}
