package com.agricultural.trace.service;

import com.agricultural.trace.config.BlockchainConfig;
import com.agricultural.trace.entity.BlockchainGasFee;
import com.agricultural.trace.entity.BlockchainTransaction;
import com.agricultural.trace.mapper.BlockchainGasFeeMapper;
import com.agricultural.trace.mapper.BlockchainTransactionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 区块链交易记录服务
 * 负责保存链上交易记录、提取Gas信息、聚合更新日统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainTransactionService {

    private final BlockchainTransactionMapper transactionMapper;
    private final BlockchainGasFeeMapper gasFeeMapper;
    private final BlockchainConfig blockchainConfig;
    private final Web3j web3j;

    /** 业务类型常量 */
    public static final int BIZ_BATCH = 1;
    public static final int BIZ_RECORD = 2;
    public static final int BIZ_INSPECTION = 3;
    public static final int BIZ_PROCESSING = 4;
    public static final int BIZ_STORAGE = 5;
    public static final int BIZ_TRANSPORT = 6;
    public static final int BIZ_SALE = 7;

    private static final BigDecimal WEI_TO_ETH = new BigDecimal("1000000000000000000");

    /**
     * 记录一笔成功的链上交易
     *
     * @param txHash       交易哈希
     * @param businessType 业务类型(1-7)
     * @param businessId   业务记录ID
     * @param batchId      批次ID
     * @param dataHash     数据哈希
     * @param receipt      交易回执(可为null)
     */
    public void recordTransaction(String txHash, int businessType, Long businessId,
                                  Long batchId, String dataHash,
                                  TransactionReceipt receipt) {
        try {
            // 幂等: 如果已存在则跳过
            LambdaQueryWrapper<BlockchainTransaction> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(BlockchainTransaction::getTxHash, txHash);
            if (transactionMapper.selectCount(existWrapper) > 0) {
                log.debug("交易记录已存在, txHash={}", txHash);
                return;
            }

            BlockchainTransaction tx = new BlockchainTransaction();
            tx.setTxHash(txHash);
            tx.setMessageId(businessType + "_" + businessId + "_" + System.currentTimeMillis());
            tx.setBusinessType(businessType);
            tx.setBusinessId(businessId);
            tx.setBatchId(batchId != null ? batchId : 0L);
            tx.setDataHash(dataHash);
            tx.setContractAddress(blockchainConfig.getContractAddress());
            tx.setStatus(1); // 成功
            tx.setChainTime(LocalDateTime.now());
            tx.setCreateTime(LocalDateTime.now());
            tx.setUpdateTime(LocalDateTime.now());

            // 从回执提取Gas信息
            long gasUsed = 0;
            long gasPriceWei = 0;
            if (receipt != null) {
                tx.setBlockNumber(receipt.getBlockNumber() != null ? receipt.getBlockNumber().longValue() : null);
                tx.setBlockHash(receipt.getBlockHash());
                tx.setFromAddress(receipt.getFrom());
                gasUsed = receipt.getGasUsed() != null ? receipt.getGasUsed().longValue() : 0;
                tx.setGasUsed(gasUsed);

                // 尝试从链上获取交易的gasPrice
                gasPriceWei = fetchGasPrice(txHash);
                tx.setGasPrice(gasPriceWei);

                if (gasUsed > 0 && gasPriceWei > 0) {
                    BigDecimal feeWei = BigDecimal.valueOf(gasUsed).multiply(BigDecimal.valueOf(gasPriceWei));
                    tx.setTransactionFee(feeWei.divide(WEI_TO_ETH, 8, RoundingMode.HALF_UP));
                }
            } else {
                // 没有回执时使用配置估算
                gasUsed = 200000L;
                gasPriceWei = (blockchainConfig.getGasPrice() != null ? blockchainConfig.getGasPrice() : 20L) * 1_000_000_000L;
                tx.setGasUsed(gasUsed);
                tx.setGasPrice(gasPriceWei);
                BigDecimal feeWei = BigDecimal.valueOf(gasUsed).multiply(BigDecimal.valueOf(gasPriceWei));
                tx.setTransactionFee(feeWei.divide(WEI_TO_ETH, 8, RoundingMode.HALF_UP));
            }

            transactionMapper.insert(tx);
            log.info("交易记录已保存, txHash={}, bizType={}, bizId={}", txHash, businessType, businessId);

            // 聚合更新日Gas统计
            updateDailyGasFee(LocalDate.now(), null, gasUsed, gasPriceWei);

        } catch (Exception e) {
            log.warn("保存交易记录失败(不影响业务): {}", e.getMessage());
        }
    }

    /**
     * 从链上获取交易的gasPrice
     */
    private long fetchGasPrice(String txHash) {
        try {
            org.web3j.protocol.core.methods.response.Transaction tx =
                    web3j.ethGetTransactionByHash(txHash).send().getTransaction().orElse(null);
            if (tx != null && tx.getGasPrice() != null) {
                return tx.getGasPrice().longValue();
            }
        } catch (Exception e) {
            log.debug("获取交易gasPrice失败: {}", e.getMessage());
        }
        // 回退到配置值
        return (blockchainConfig.getGasPrice() != null ? blockchainConfig.getGasPrice() : 20L) * 1_000_000_000L;
    }

    /**
     * 聚合更新日Gas费用统计
     */
    private void updateDailyGasFee(LocalDate date, Long enterpriseId, long gasUsed, long gasPriceWei) {
        try {
            LambdaQueryWrapper<BlockchainGasFee> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BlockchainGasFee::getStatDate, date);
            if (enterpriseId != null) {
                wrapper.eq(BlockchainGasFee::getEnterpriseId, enterpriseId);
            } else {
                wrapper.isNull(BlockchainGasFee::getEnterpriseId);
            }

            BlockchainGasFee fee = gasFeeMapper.selectOne(wrapper);
            BigDecimal feeWei = BigDecimal.valueOf(gasUsed).multiply(BigDecimal.valueOf(gasPriceWei));
            BigDecimal feeEth = feeWei.divide(WEI_TO_ETH, 8, RoundingMode.HALF_UP);

            if (fee == null) {
                fee = new BlockchainGasFee();
                fee.setStatDate(date);
                fee.setEnterpriseId(enterpriseId);
                fee.setTransactionCount(1);
                fee.setTotalGasUsed(gasUsed);
                fee.setTotalFeeWei(feeWei);
                fee.setTotalFeeEth(feeEth);
                fee.setAvgGasPrice(gasPriceWei);
                fee.setCreateTime(LocalDateTime.now());
                fee.setUpdateTime(LocalDateTime.now());
                gasFeeMapper.insert(fee);
            } else {
                fee.setTransactionCount(fee.getTransactionCount() + 1);
                fee.setTotalGasUsed(fee.getTotalGasUsed() + gasUsed);
                fee.setTotalFeeWei(fee.getTotalFeeWei().add(feeWei));
                fee.setTotalFeeEth(fee.getTotalFeeEth().add(feeEth));
                // 更新平均gasPrice
                fee.setAvgGasPrice((fee.getAvgGasPrice() + gasPriceWei) / 2);
                fee.setUpdateTime(LocalDateTime.now());
                gasFeeMapper.updateById(fee);
            }
        } catch (Exception e) {
            log.warn("更新日Gas统计失败: {}", e.getMessage());
        }
    }
}
