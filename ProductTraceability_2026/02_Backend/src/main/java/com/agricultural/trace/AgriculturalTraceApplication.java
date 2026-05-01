package com.agricultural.trace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 农产品溯源系统启动类
 */
@SpringBootApplication
@MapperScan("com.agricultural.trace.mapper")
@EnableScheduling
public class AgriculturalTraceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgriculturalTraceApplication.class, args);
        System.out.println("========================================");
        System.out.println("农产品溯源系统启动成功");
        System.out.println("========================================");
    }
}
