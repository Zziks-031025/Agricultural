package com.agricultural.trace.service;

import com.agricultural.trace.config.BlockchainConfig;
import com.agricultural.trace.entity.BlockchainContract;
import com.agricultural.trace.mapper.BlockchainContractMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 区块链节点状态服务
 * 提供节点连接状态、最新区块、合约配置等信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainNodeService {

    private final Web3j web3j;
    private final BlockchainConfig blockchainConfig;
    private final BlockchainContractMapper contractMapper;

    /**
     * 获取节点状态信息
     */
    public Map<String, Object> getNodeStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        // 1. 检测RPC连接
        boolean rpcReachable = false;
        String clientVersion = null;
        Long chainId = null;
        try {
            clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            rpcReachable = clientVersion != null && !clientVersion.isEmpty();
        } catch (Exception e) {
            log.warn("RPC连接检测失败: {}", e.getMessage());
        }

        try {
            chainId = web3j.ethChainId().send().getChainId().longValue();
        } catch (Exception e) {
            log.debug("获取chainId失败: {}", e.getMessage());
        }

        status.put("rpcReachable", rpcReachable);
        status.put("clientVersion", clientVersion);
        status.put("chainId", chainId);
        status.put("rpcUrl", blockchainConfig.getRpcUrl());

        // 2. 最新区块信息
        Long latestBlockNumber = null;
        String latestBlockTime = null;
        try {
            EthBlock ethBlock = web3j.ethGetBlockByNumber(
                    DefaultBlockParameterName.LATEST, false).send();
            if (ethBlock.getBlock() != null) {
                latestBlockNumber = ethBlock.getBlock().getNumber().longValue();
                long timestamp = ethBlock.getBlock().getTimestamp().longValue();
                LocalDateTime blockTime = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                latestBlockTime = blockTime.toString();
            }
        } catch (Exception e) {
            log.warn("获取最新区块失败: {}", e.getMessage());
        }
        status.put("latestBlockNumber", latestBlockNumber);
        status.put("latestBlockTime", latestBlockTime);

        // 3. 合约配置/部署状态
        String contractAddress = blockchainConfig.getContractAddress();
        boolean contractConfigured = contractAddress != null && !contractAddress.isEmpty();
        boolean contractDeployed = false;

        if (contractConfigured) {
            // 检查数据库中是否有活跃合约记录
            LambdaQueryWrapper<BlockchainContract> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BlockchainContract::getStatus, 1);
            wrapper.eq(BlockchainContract::getContractAddress, contractAddress);
            contractDeployed = contractMapper.selectCount(wrapper) > 0;

            // 如果数据库没有记录，尝试检查链上合约代码
            if (!contractDeployed) {
                try {
                    String code = web3j.ethGetCode(contractAddress,
                            DefaultBlockParameterName.LATEST).send().getCode();
                    contractDeployed = code != null && !code.equals("0x") && !code.equals("0x0");
                } catch (Exception e) {
                    log.debug("检查合约代码失败: {}", e.getMessage());
                }
            }
        }

        status.put("contractAddress", contractAddress);
        status.put("contractConfigured", contractConfigured);
        status.put("contractDeployed", contractDeployed);

        return status;
    }
}
