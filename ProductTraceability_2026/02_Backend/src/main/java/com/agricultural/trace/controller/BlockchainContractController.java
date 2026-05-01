package com.agricultural.trace.controller;

import com.agricultural.trace.annotation.OperationLog;
import com.agricultural.trace.common.Result;
import com.agricultural.trace.config.BlockchainConfig;
import com.agricultural.trace.entity.BlockchainContract;
import com.agricultural.trace.mapper.BlockchainContractMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Credentials;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/blockchain/contract")
@RequiredArgsConstructor
@CrossOrigin
public class BlockchainContractController {

    private final BlockchainContractMapper contractMapper;
    private final JdbcTemplate jdbcTemplate;
    private final BlockchainConfig blockchainConfig;
    
    @Autowired(required = false)
    private Credentials credentials;

    /**
     * 获取合约信息
     */
    /**
     * 获取合约部署信息
     * 返回合约名称、地址、部署交易哈希、部署区块、网络、部署时间、状态
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getContractInfo() {
        try {
            Map<String, Object> info = new LinkedHashMap<>();

            LambdaQueryWrapper<BlockchainContract> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BlockchainContract::getStatus, 1);
            wrapper.orderByDesc(BlockchainContract::getCreateTime);
            wrapper.last("LIMIT 1");
            BlockchainContract contract = contractMapper.selectOne(wrapper);

            if (contract != null) {
                info.put("contractName", contract.getContractName());
                info.put("contractAddress", contract.getContractAddress());
                info.put("deployTxHash", contract.getDeployTxHash());
                info.put("deployBlockNumber", contract.getDeployBlockNumber());
                info.put("network", contract.getNetwork());
                info.put("deployTime", contract.getCreateTime());
                info.put("status", contract.getStatus());
            } else {
                info.put("contractName", "Traceability");
                info.put("contractAddress", blockchainConfig.getContractAddress());
                info.put("deployTxHash", null);
                info.put("deployBlockNumber", null);
                info.put("network", "Ethereum (Ganache)");
                info.put("deployTime", null);
                info.put("status", 1);
            }

            info.put("deployed", info.get("contractAddress") != null && !info.get("contractAddress").toString().isEmpty());

            return Result.success(info);
        } catch (Exception e) {
            log.error("获取合约信息失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取授权操作员列表
     */
    @GetMapping("/operators")
    public Result<List<Map<String, Object>>> getOperators() {
        try {
            List<Map<String, Object>> operators = new ArrayList<>();
            
            List<Map<String, Object>> enterpriseOps = jdbcTemplate.queryForList(
                "SELECT wallet_address AS address, enterprise_name AS name, " +
                "create_time AS addTime, status AS authorized " +
                "FROM enterprise_info WHERE wallet_address IS NOT NULL AND wallet_address != '' " +
                "ORDER BY create_time DESC");
            
            for (Map<String, Object> op : enterpriseOps) {
                Object statusVal = op.get("authorized");
                if (statusVal instanceof Number) {
                    op.put("authorized", ((Number) statusVal).intValue() == 1);
                }
                operators.add(op);
            }
            
            List<Map<String, Object>> configOps = jdbcTemplate.queryForList(
                "SELECT config_value AS address, " +
                "SUBSTRING_INDEX(description, ': ', -1) AS name, " +
                "create_time AS addTime, 1 AS authorized " +
                "FROM sys_config WHERE config_key LIKE 'blockchain.operator.%' " +
                "ORDER BY create_time DESC");
            
            for (Map<String, Object> op : configOps) {
                op.put("authorized", true);
                operators.add(op);
            }
            
            return Result.success(operators);
        } catch (Exception e) {
            log.error("获取操作员列表失败: {}", e.getMessage());
            return Result.success(new ArrayList<>());
        }
    }

    /**
     * 添加操作员（设置企业钱包地址）
     */
    @OperationLog(module = "区块链管理", operation = "添加操作员")
    @PostMapping("/operator/add")
    public Result<Void> addOperator(@RequestBody Map<String, String> body) {
        try {
            String address = body.get("address");
            String name = body.get("name");
            if (address == null || address.isEmpty()) {
                return Result.error("钱包地址不能为空");
            }

            // 查找匹配企业名称的记录，更新钱包地址
            if (name != null && !name.isEmpty()) {
                int updated = jdbcTemplate.update(
                    "UPDATE enterprise_info SET wallet_address = ? WHERE enterprise_name = ? AND (wallet_address IS NULL OR wallet_address = '')",
                    address, name);
                if (updated > 0) {
                    log.info("操作员钱包地址已关联企业, address={}, enterprise={}", address, name);
                    return Result.success();
                }
            }

            // 没有匹配到企业，在sys_config中记录操作员
            jdbcTemplate.update(
                "INSERT INTO sys_config (config_key, config_value, config_type, description, create_time, update_time) " +
                "VALUES (?, ?, 'blockchain', ?, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), update_time = NOW()",
                "blockchain.operator." + address.toLowerCase(), address,
                "授权操作员: " + (name != null ? name : address));
            log.info("添加操作员成功, address={}", address);
            return Result.success();
        } catch (Exception e) {
            log.error("添加操作员失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 撤销操作员授权
     */
    @OperationLog(module = "区块链管理", operation = "撤销操作员授权")
    @PostMapping("/operator/revoke")
    public Result<Void> revokeOperator(@RequestBody Map<String, String> body) {
        try {
            String address = body.get("address");
            if (address == null || address.isEmpty()) {
                return Result.error("钱包地址不能为空");
            }

            // 清除企业的钱包地址
            int updated = jdbcTemplate.update(
                "UPDATE enterprise_info SET wallet_address = NULL WHERE wallet_address = ?", address);
            if (updated == 0) {
                // 尝试删除sys_config中的记录
                jdbcTemplate.update(
                    "DELETE FROM sys_config WHERE config_key = ?",
                    "blockchain.operator." + address.toLowerCase());
            }
            log.info("撤销操作员授权, address={}", address);
            return Result.success();
        } catch (Exception e) {
            log.error("撤销操作员失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
