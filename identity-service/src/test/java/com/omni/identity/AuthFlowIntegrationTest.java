package com.omni.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.identity.auth.EmailService;
import com.omni.identity.auth.dto.LoginRequest;
import com.omni.identity.auth.dto.RegisterRequest;
import com.omni.identity.user.Role;
import com.omni.identity.user.RoleRepository;
import com.omni.identity.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * TÁC DỤNG: Test toàn bộ luồng xác thực (End-to-End Auth Flow).
 * Bao gồm: Đăng ký -> Nhận email xác minh -> Kích hoạt tài khoản -> Đăng nhập (lấy JWT) -> Gọi API bảo mật.
 * Sử dụng H2 Database (In-memory) để chạy độc lập, không cần khởi động Postgres thật.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest {

    // Cấu hình H2 Database và tắt các dịch vụ ngoại vi (Eureka, Flyway) để test chạy nhanh và độc lập
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.jwt.secret", () -> "omni-ecom-microservices-super-secret-key-2026-local-dev");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired private TestRestTemplate http;
    @Autowired private ObjectMapper om;
    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;

    // Mock EmailService để không gửi mail thật, chỉ bắt lấy token
    @MockitoBean
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<String> tokenCaptor;

    @BeforeEach
    void setUp() {
        // ARRANGE: Mồi dữ liệu Role vào H2 DB (vì đã tắt Flyway nên DB trống)
        if (roleRepo.findByCode("CUSTOMER").isEmpty()) {
            Role customer = new Role();
            customer.setId(UUID.randomUUID()); // Role không có @GeneratedValue nên phải set ID thủ công
            customer.setCode("CUSTOMER");
            customer.setName("Customer");
            roleRepo.save(customer);
        }
        if (roleRepo.findByCode("ADMIN").isEmpty()) {
            Role admin = new Role();
            admin.setId(UUID.randomUUID());
            admin.setCode("ADMIN");
            admin.setName("Admin");
            roleRepo.save(admin);
        }
    }

    @Test
    @DisplayName("Full Auth Flow: Register -> Verify -> Login -> Access Protected API")
    void fullAuthFlow() throws Exception {
        String email = "test." + UUID.randomUUID() + "@omni.com";
        String password = "Password123@";

        // ================= ACT 1: Đăng ký tài khoản =================
        var registerReq = new RegisterRequest(email, "0901234567", password, "Test User");
        ResponseEntity<String> regResp = http.postForEntity("/api/v1/auth/register", registerReq, String.class);
        assertThat(regResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ================= ASSERT 1: Bắt token từ email giả lập =================
        verify(emailService).sendVerifyEmail(eq(email), tokenCaptor.capture());
        String verificationToken = tokenCaptor.getValue();
        assertThat(verificationToken).isNotBlank();

        // ================= ACT 2: Gọi API kích hoạt tài khoản =================
        ResponseEntity<String> verifyResp = http.getForEntity(
                "/api/v1/auth/verify-email?token=" + verificationToken, String.class);
        assertThat(verifyResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ================= ACT 3: Đăng nhập để lấy JWT =================
        var loginReq = new LoginRequest(email, password);
        ResponseEntity<String> loginResp = http.postForEntity("/api/v1/auth/login", loginReq, String.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode json = om.readTree(loginResp.getBody());
        String accessToken = json.path("data").path("tokens").path("accessToken").asText();

        // ================= ACT 4 & ASSERT 3: Dùng JWT gọi API bảo mật =================
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        var request = new org.springframework.http.HttpEntity<>(headers);

        ResponseEntity<String> meResp = http.exchange("/api/v1/users/me", HttpMethod.GET, request, String.class);
        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResp.getBody()).contains(email);
    }
}