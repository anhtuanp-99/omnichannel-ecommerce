package com.omni.orderinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.omni.orderinventory.integration")
@EnableCaching
@EnableScheduling
@Import(com.omni.common.web.GlobalExceptionHandler.class)
public class OrderInventoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderInventoryApplication.class, args);
    }
}