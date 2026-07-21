package com.jicek.license;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 极策k网络验证 - 启动类
 * 作者: 极策k  日期: 2026-07-21
 */
@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.jicek.license.**.mapper")
public class JicekLicenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(JicekLicenseApplication.class, args);
    }
}
