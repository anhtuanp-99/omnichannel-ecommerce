package com.omni.payment;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Lớp cơ sở cho tất cả các bài Integration Test của Payment Service.
 * Khởi tạo môi trường Database (PostgreSQL) và Message Broker (Kafka) độc lập.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("payment_db");
    // Đã xóa .withReuse(true) để tránh lỗi Connection refused khi chạy nhiều test

    @Container
    public static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
    // Đã đổi sang ConfluentKafkaContainer (bản mới) và xóa .withReuse(true)

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Ghi đè cấu hình kết nối DB
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Ghi đè cấu hình kết nối Kafka
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}