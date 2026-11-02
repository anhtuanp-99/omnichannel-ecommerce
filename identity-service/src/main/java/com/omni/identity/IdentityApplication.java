package com.omni.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDiscoveryClient
@Import(com.omni.common.web.GlobalExceptionHandler.class)
public class IdentityApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}