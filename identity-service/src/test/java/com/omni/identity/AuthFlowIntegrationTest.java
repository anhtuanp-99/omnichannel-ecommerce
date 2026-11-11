package com.omni.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.identity.auth.EmailService;
import com.omni.identity.auth.dto.LoginRequest;
import com.omni.identity.auth.dto.RegisterRequest;
import com.omni.identity.user.User;
import com.omni.identity.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Tự động rollback dữ liệu sau khi chạy xong, không để lại rác trong database
class AuthFlowIntegrationTest {

    private static final String TEST_PASSWORD = "Password123@";
    private static final String TEST_PHONE = "0901234567";
    private static final String TEST_FULL_NAME = "Anh Tuan";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5433/postgres?currentSchema=public");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "anhtuansql");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "0");
        registry.add("app.jwt.secret", () -> "omni-ecom-microservices-super-secret-key-2026-local-dev");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserRepository userRepo;

    @MockitoBean
    private EmailService emailService;

    @Test
    @DisplayName("E2E Auth Flow: Register -> Block unverified -> Activate -> Login -> Get /me")
    void fullFlow_registerVerifyLoginMe() throws Exception {
        String email = "test." + System.nanoTime() + "@omni.com";
        doNothing().when(emailService).sendVerifyEmail(anyString(), anyString());

        // 1. Đăng ký tài khoản mới
        var registerRequest = new RegisterRequest(email, TEST_PHONE, TEST_PASSWORD, TEST_FULL_NAME);
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // 2. Chặn đăng nhập khi chưa kích hoạt email (trả về lỗi 4xx)
        var unverifiedLoginRequest = new LoginRequest(email, TEST_PASSWORD);
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(unverifiedLoginRequest)))
                .andExpect(status().is4xxClientError());

        // 3. Kích hoạt tài khoản trực tiếp trong DB test
        User user = userRepo.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new AssertionError("Không tìm thấy user sau khi đăng ký: " + email));
        user.setEmailVerified(true);
        user.setStatus(User.Status.ACTIVE);
        userRepo.save(user);

        // 4. Đăng nhập sau khi kích hoạt -> Nhận cặp Access Token & Refresh Token
        var validLoginRequest = new LoginRequest(email, TEST_PASSWORD);
        String loginResponseJson = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokens.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokens.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String accessToken = om.readTree(loginResponseJson)
                .path("data").path("tokens").path("accessToken").asText();

        // 5. Xác thực với Access Token tại endpoint bảo mật GET /api/v1/users/me
        mvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.roles").isArray());
    }
}