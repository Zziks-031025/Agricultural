package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.dto.BatchQueryDTO;
import com.agricultural.trace.dto.BatchReceiveDTO;
import com.agricultural.trace.service.BatchService;
import com.agricultural.trace.vo.BatchVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 批次管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@CrossOrigin
public class BatchController {

    private final BatchService batchService;

    /**
     * 分页查询批次列表
     *
     * 支持多状态查询，前端可通过 statuses 参数传入状态数组：
     * - 养殖中 Tab: statuses=1,2
     * - 待检疫 Tab: statuses=3
     * - 已完结 Tab: statuses=4,5,6,7,8
     *
     * @param current 当前页
     * @param size 每页大小
     * @param enterpriseId 企业ID（可选）
     * @param keyword 搜索关键词（可选，同时搜索批次号和产品名称）
     * @param statuses 状态列表（可选，支持多个状态）
     * @param productType 产品类型（可选）
     * @param hasChain 是否已上链（可选）
     * @return 批次分页列表
     */
    @GetMapping("/list")
    public Result<Page<BatchVO>> getBatchList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) Integer enterpriseType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Integer> statuses,
            @RequestParam(required = false) Integer productType,
            @RequestParam(required = false) Boolean hasChain,
            @RequestParam(required = false) Boolean excludeStorageEntered,
            @RequestParam(required = false) Boolean excludeProcessedCreated,
            @RequestParam(required = false) Long inspectionEnterpriseId
    ) {
        log.info(
                "查询批次列表，current: {}, size: {}, enterpriseId: {}, enterpriseType: {}, keyword: {}, statuses: {}, productType: {}, hasChain: {}, excludeStorageEntered: {}, inspectionEnterpriseId: {}",
                current, size, enterpriseId, enterpriseType, keyword, statuses, productType, hasChain, excludeStorageEntered, inspectionEnterpriseId
        );

        BatchQueryDTO queryDTO = new BatchQueryDTO();
        queryDTO.setCurrent(current);
        queryDTO.setSize(size);
        queryDTO.setEnterpriseId(enterpriseId);
        queryDTO.setEnterpriseType(enterpriseType);
        queryDTO.setKeyword(keyword);
        queryDTO.setStatuses(statuses);
        queryDTO.setProductType(productType);
        queryDTO.setHasChain(hasChain);
        queryDTO.setExcludeStorageEntered(excludeStorageEntered);
        queryDTO.setExcludeProcessedCreated(excludeProcessedCreated);
        queryDTO.setInspectionEnterpriseId(inspectionEnterpriseId);

        Page<BatchVO> page = batchService.getBatchList(queryDTO);

        return Result.success(page);
    }

    /**
     * 获取各状态的批次数量统计
     *
     * 返回格式：
     * {
     *   "breeding": 5,   // 养殖中数量（状态1,2）
     *   "pending": 2,    // 待检疫数量（状态3）
     *   "completed": 10  // 已完结数量（状态4,5,6,7,8）
     * }
     *
     * @param enterpriseId 企业ID（可选，不传则统计全部）
     * @return 状态数量统计
     */
    @GetMapping("/status-count")
    public Result<Map<String, Long>> getStatusCount(
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) Integer enterpriseType
    ) {
        log.info("查询批次状态统计，enterpriseId: {}, enterpriseType: {}", enterpriseId, enterpriseType);

        Map<String, Long> countMap = batchService.getStatusCount(enterpriseId, enterpriseType);

        return Result.success(countMap);
    }

    /**
     * 获取批次详细信息
     *
     * 支持两种查询方式（按优先级）：
     * 1. 通过批次编号 (batchCode) 查询
     * 2. 通过批次ID (id) 查询
     *
     * 所有企业角色均可调用此接口，检疫员也可查看上游批次详情。
     *
     * 请求示例：
     * GET /api/batch/detail?batchCode=BATCH202602010001
     * GET /api/batch/detail?id=1001
     *
     * @param batchCode 批次编号（优先）
     * @param id 批次ID
     * @return 批次完整信息
     */
    @GetMapping("/detail")
    public Result<Map<String, Object>> getBatchDetail(
            @RequestParam(required = false) String batchCode,
            @RequestParam(required = false) Long id
    ) {
        log.info("查询批次详情，batchCode: {}, id: {}", batchCode, id);

        try {
            Map<String, Object> detail = batchService.getBatchDetail(batchCode, id);
            return Result.success(detail);
        } catch (RuntimeException e) {
            log.error("查询批次详情失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建新批次 (同时支持 /create 和 /add 路径)
     */
    @OperationLog("创建批次")
    @PostMapping({"/create", "/add"})
    public Result<Map<String, Object>> createBatch(@RequestBody Map<String, Object> params) {
        log.info("创建批次, params: {}", params);
        try {
            Map<String, Object> result = batchService.createBatch(params);
            return Result.success("批次创建成功", result);
        } catch (Exception e) {
            log.error("创建批次失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检疫状态查询
     * 根据批次编号查询该批次的检疫合格状态
     *
     * @param batchCode 批次编号
     * @return 批次信息及检疫状态
     */
    @GetMapping("/quarantine-check/{batchCode}")
    public Result<Map<String, Object>> quarantineCheck(@PathVariable String batchCode) {
        log.info("查询批次检疫状态，batchCode: {}", batchCode);

        try {
            Map<String, Object> result = batchService.checkQuarantine(batchCode);
            return Result.success(result);
        } catch (RuntimeException e) {
            log.error("查询检疫状态失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 扫码接收批次
     * 加工企业扫码接收养殖批次，将批次状态更新为"加工中"(4)
     * 前置条件：该批次必须拥有检疫合格记录
     *
     * @param dto 接收数据
     * @return 接收结果
     */
    /**
     * 删除批次
     * 仅允许删除状态为初始化(1)的批次
     */
    @OperationLog("删除批次")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteBatch(@PathVariable Long id) {
        log.info("删除批次, id: {}", id);
        try {
            batchService.deleteBatch(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除批次失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @OperationLog("扫码接收批次")
    @PostMapping("/receive")
    public Result<Map<String, Object>> receiveBatch(@Validated @RequestBody BatchReceiveDTO dto) {
        log.info("扫码接收批次，batchCode: {}, receiver: {}, quantity: {}",
                dto.getBatchCode(), dto.getReceiver(), dto.getReceiveQuantity());

        try {
            Map<String, Object> result = batchService.receiveBatch(dto);
            return Result.success("接收成功", result);
        } catch (RuntimeException e) {
            log.error("接收批次失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消接收批次
     * 将批次状态从"加工中"(4)恢复为"已收获"(3)
     *
     * @param batchId 批次ID
     * @return 操作结果
     */
    @OperationLog("取消接收批次")
    @PostMapping("/cancel-receive/{batchId}")
    public Result<String> cancelReceive(@PathVariable Long batchId) {
        log.info("取消接收批次, batchId: {}", batchId);
        try {
            batchService.cancelReceive(batchId);
            return Result.success("已取消接收");
        } catch (RuntimeException e) {
            log.error("取消接收失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 拒绝接收批次
     * 加工企业拒绝接收运输中的批次，回滚状态并通知养殖企业
     */
    @OperationLog("拒绝接收批次")
    @PostMapping("/reject/{batchId}")
    public Result<String> rejectBatch(
            @PathVariable Long batchId,
            @RequestBody Map<String, Object> params) {
        String reason = params.get("reason") != null ? params.get("reason").toString() : "";
        Long enterpriseId = params.get("enterpriseId") != null ? Long.parseLong(params.get("enterpriseId").toString()) : null;
        log.info("拒绝接收批次, batchId: {}, reason: {}, enterpriseId: {}", batchId, reason, enterpriseId);
        try {
            batchService.rejectBatch(batchId, reason, enterpriseId);
            return Result.success("已拒绝接收");
        } catch (RuntimeException e) {
            log.error("拒绝接收失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
