package com.agricultural.trace.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import javax.annotation.PostConstruct;
import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Web3j 工具类
 * 用于与以太坊区块链交互，实现智能合约调用
 */
@Slf4j
@Component
public class Web3jUtils {

    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.contract-address}")
    private String contractAddress;

    @Value("${blockchain.wallet-path:}")
    private String walletPath;

    @Value("${blockchain.wallet-password:}")
    private String walletPassword;

    @Value("${blockchain.private-key:}")
    private String privateKey;

    @Value("${blockchain.account-address:}")
    private String accountAddress;

    @Value("${blockchain.gas-limit:300000}")
    private Long gasLimit;

    @Value("${blockchain.gas-price:20}")
    private Long gasPrice;

    private Web3j web3j;
    private Credentials credentials;
    private TransactionManager txManager;
    private String senderAddress;

    /**
     * 初始化 Web3j 连接和钱包凭证
     */
    @PostConstruct
    public void init() {
        try {
            // 初始化 Web3j 连接
            web3j = Web3j.build(new HttpService(rpcUrl));
            log.info("Web3j 连接初始化成功，RPC URL: {}", rpcUrl);

            // 优先使用 account-address 模式（Ganache unlocked 账户，无需本地签名）
            if (accountAddress != null && !accountAddress.isEmpty()) {
                senderAddress = accountAddress;
                txManager = new org.web3j.tx.ClientTransactionManager(web3j, accountAddress);
                log.info("使用 Ganache unlocked 账户模式，地址: {}", accountAddress);
            } else {
                // 回退到私钥签名模式
                loadCredentials();
                senderAddress = credentials.getAddress();
                long chainId = web3j.ethChainId().send().getChainId().longValue();
                txManager = new RawTransactionManager(web3j, credentials, chainId);
                log.info("使用私钥签名模式，地址: {}", senderAddress);
            }

        } catch (Exception e) {
            log.error("Web3j 初始化失败", e);
            throw new RuntimeException("区块链连接初始化失败", e);
        }
    }

    /**
     * 加载钱包凭证
     * 支持两种方式：1. 钱包文件 2. 私钥
     */
    private void loadCredentials() throws Exception {
        if (privateKey != null && !privateKey.isEmpty()) {
            // 方式1：通过私钥加载
            credentials = Credentials.create(privateKey);
            log.info("通过私钥加载钱包凭证");
        } else if (walletPath != null && !walletPath.isEmpty()) {
            // 方式2：通过钱包文件加载
            File walletFile = new File(walletPath);
            if (!walletFile.exists()) {
                throw new RuntimeException("钱包文件不存在: " + walletPath);
            }
            credentials = WalletUtils.loadCredentials(walletPassword, walletFile);
            log.info("通过钱包文件加载凭证");
        } else {
            throw new RuntimeException("未配置钱包凭证，请配置 private-key 或 wallet-path");
        }
    }

    /**
     * 调用智能合约的 uploadHash 方法
     * 将溯源数据哈希上传到区块链
     *
     * @param traceId  溯源ID/批次号
     * @param dataHash 数据哈希值（SHA-256）
     * @return 交易哈希
     */
    public String uploadHash(String traceId, String dataHash) {
        try {
            log.info("开始上传哈希到区块链，traceId: {}, dataHash: {}", traceId, dataHash);

            // 构建函数调用
            Function function = new Function(
                    "uploadHash",
                    Arrays.asList(
                            new Utf8String(traceId),
                            new Utf8String(dataHash)
                    ),
                    Collections.singletonList(new TypeReference<org.web3j.abi.datatypes.Bool>() {})
            );

            // 编码函数调用数据
            String encodedFunction = FunctionEncoder.encode(function);

            // 使用预初始化的 TransactionManager 发送交易
            EthSendTransaction ethSendTransaction = txManager.sendTransaction(
                    BigInteger.valueOf(gasPrice * 1_000_000_000L), // Gas Price (Gwei to Wei)
                    BigInteger.valueOf(gasLimit),
                    contractAddress,
                    encodedFunction,
                    BigInteger.ZERO // Value (不发送 ETH)
            );

            if (ethSendTransaction.hasError()) {
                String errorMsg = ethSendTransaction.getError().getMessage();
                log.error("交易发送失败: {}", errorMsg);
                throw new RuntimeException("区块链交易失败: " + errorMsg);
            }

            String txHash = ethSendTransaction.getTransactionHash();
            log.info("交易发送成功，交易哈希: {}", txHash);

            return txHash;

        } catch (Exception e) {
            log.error("上传哈希到区块链失败", e);
            throw new RuntimeException("区块链上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用智能合约的 getHash 方法
     * 从区块链查询溯源数据哈希
     *
     * @param traceId 溯源ID/批次号
     * @return 区块链记录对象
     */
    public BlockchainRecord getHash(String traceId) {
        try {
            log.info("从区块链查询哈希，traceId: {}", traceId);

            // 构建函数调用
            Function function = new Function(
                    "getHash",
                    Collections.singletonList(new Utf8String(traceId)),
                    Arrays.asList(
                            new TypeReference<Utf8String>() {},  // traceId
                            new TypeReference<Utf8String>() {},  // dataHash
                            new TypeReference<org.web3j.abi.datatypes.Address>() {},  // operator
                            new TypeReference<Uint256>() {}      // timestamp
                    )
            );

            // 编码函数调用
            String encodedFunction = FunctionEncoder.encode(function);

            // 调用合约（只读操作，不消耗 Gas）
            org.web3j.protocol.core.methods.response.EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            senderAddress,
                            contractAddress,
                            encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                String errorMsg = response.getError().getMessage();
                log.error("查询失败: {}", errorMsg);
                throw new RuntimeException("区块链查询失败: " + errorMsg);
            }

            // 解码返回值
            List<Type> results = FunctionReturnDecoder.decode(
                    response.getValue(),
                    function.getOutputParameters()
            );

            if (results.isEmpty()) {
                throw new RuntimeException("溯源记录不存在");
            }

            BlockchainRecord record = new BlockchainRecord();
            record.setTraceId(results.get(0).getValue().toString());
            record.setDataHash(results.get(1).getValue().toString());
            record.setOperator(results.get(2).getValue().toString());
            record.setTimestamp(((BigInteger) results.get(3).getValue()).longValue());

            log.info("查询成功，dataHash: {}", record.getDataHash());
            return record;

        } catch (Exception e) {
            log.error("从区块链查询哈希失败", e);
            throw new RuntimeException("区块链查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证数据哈希是否匹配
     *
     * @param traceId  溯源ID
     * @param dataHash 待验证的数据哈希
     * @return 是否匹配
     */
    public boolean verifyHash(String traceId, String dataHash) {
        try {
            BlockchainRecord record = getHash(traceId);
            boolean isMatch = record.getDataHash().equalsIgnoreCase(dataHash);
            log.info("哈希验证结果: {}, traceId: {}", isMatch ? "匹配" : "不匹配", traceId);
            return isMatch;
        } catch (Exception e) {
            log.warn("哈希验证失败(traceId={}): {}", traceId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取交易回执
     *
     * @param txHash 交易哈希
     * @return 交易回执
     */
    public TransactionReceipt getTransactionReceipt(String txHash) {
        try {
            return web3j.ethGetTransactionReceipt(txHash)
                    .send()
                    .getTransactionReceipt()
                    .orElse(null);
        } catch (Exception e) {
            log.error("获取交易回执失败", e);
            return null;
        }
    }

    /**
     * 区块链记录对象
     */
    @lombok.Data
    public static class BlockchainRecord {
        private String traceId;
        private String dataHash;
        private String operator;
        private Long timestamp;
    }
}
