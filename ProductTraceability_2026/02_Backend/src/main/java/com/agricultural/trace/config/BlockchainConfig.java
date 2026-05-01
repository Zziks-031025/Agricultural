package com.agricultural.trace.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

/**
 * 区块链配置类
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "blockchain")
public class BlockchainConfig {

    private String rpcUrl;
    private String contractAddress;
    private String accountAddress;
    private String privateKey;
    private String walletPath;
    private String walletPassword;
    private Long gasLimit;
    private Long gasPrice;

    /**
     * 创建 Web3j 实例
     */
    @Bean
    public Web3j web3j() {
        log.info("初始化 Web3j 连接，RPC URL: {}", rpcUrl);
        Web3j web3j = Web3j.build(new HttpService(rpcUrl));
        
        try {
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            log.info("Web3j 连接成功，客户端版本: {}", clientVersion);
        } catch (Exception e) {
            log.error("Web3j 连接失败", e);
        }
        
        return web3j;
    }

    /**
     * 创建钱包凭证
     */
    @Bean
    public Credentials credentials() {
        if (privateKey == null || privateKey.isEmpty()) {
            log.warn("未配置私钥，区块链写入功能将不可用");
            return null;
        }
        
        try {
            Credentials credentials = Credentials.create(privateKey);
            log.info("钱包凭证加载成功，地址: {}", credentials.getAddress());
            return credentials;
        } catch (Exception e) {
            log.error("钱包凭证加载失败", e);
            throw new RuntimeException("钱包凭证加载失败", e);
        }
    }

    /**
     * 创建 Gas 提供者
     */
    @Bean
    public DefaultGasProvider gasProvider() {
        if (gasPrice != null && gasLimit != null) {
            return new DefaultGasProvider() {
                @Override
                public java.math.BigInteger getGasPrice(String contractFunc) {
                    return java.math.BigInteger.valueOf(gasPrice * 1_000_000_000L);
                }

                @Override
                public java.math.BigInteger getGasLimit(String contractFunc) {
                    return java.math.BigInteger.valueOf(gasLimit);
                }
            };
        }
        return new DefaultGasProvider();
    }
}
