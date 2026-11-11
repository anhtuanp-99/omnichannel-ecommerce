package com.omni.identity;

import com.omni.identity.auth.RefreshTokenService;
import com.omni.identity.user.RefreshToken;
import com.omni.identity.user.RefreshTokenRepository;
import com.omni.identity.user.User;
import com.omni.identity.user.UserRepository;
import com.omni.identity.util.HashUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional // Tự động rollback dữ liệu sau khi chạy xong, giữ sạch database
class RefreshTokenRotationTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5433/postgres?currentSchema=public");
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "anhtuansql");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.flyway.baseline-on-migrate", () -> "true");
        r.add("spring.flyway.baseline-version", () -> "0");
        r.add("app.jwt.secret", () -> "omni-ecom-microservices-super-secret-key-2026-local-dev");
        r.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private RefreshTokenService svc;

    @Autowired
    private RefreshTokenRepository repo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private EntityManager entityManager;

    /**
     * Tạo và lưu User hợp lệ vào DB để thỏa mãn ràng buộc Foreign Key của bảng refresh_tokens
     */
    private User createTestUser() {
        User user = new User();
        user.setEmail("user." + System.nanoTime() + "@omni.com");
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF");
        user.setFullName("Test User");
        user.setPhone("09" + (System.currentTimeMillis() % 100000000L));
        user.setStatus(User.Status.ACTIVE);
        user.setEmailVerified(true);
        return userRepo.save(user);
    }

    @Test
    @DisplayName("Xoay vòng token: Token cũ phải bị thu hồi và có liên kết replacedBy")
    void rotate_revokesOldToken() {
        User testUser = createTestUser();
        String old = svc.create(testUser.getId(), "UA", "127.0.0.1");

        var res = svc.rotate(old);
        assertThat(res.newRefreshToken()).isNotEqualTo(old);

        // Xóa First-Level Cache để ép Hibernate đọc bản ghi mới nhất từ DB
        entityManager.flush();
        entityManager.clear();

        RefreshToken oldToken = repo.findByTokenHash(HashUtil.sha256Hex(old))
                .orElseThrow(() -> new AssertionError("Không tìm thấy token cũ"));
        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(oldToken.getReplacedBy()).isNotNull();
    }

    @Test
    @DisplayName("Phát hiện dùng lại token: Khi dùng lại token đã thu hồi, toàn bộ token của user phải bị thu hồi")
    void reuseDetection_revokesAllUserTokens() {
        User testUser = createTestUser();
        String t1 = svc.create(testUser.getId(), "UA", "127.0.0.1");
        svc.rotate(t1); // t1 đã bị revoke, token mới được sinh ra

        String t2 = svc.create(testUser.getId(), "UA", "127.0.0.1"); // token hợp lệ thứ hai

        // Dùng lại t1 -> ném exception cảnh báo thu hồi
        assertThatThrownBy(() -> svc.rotate(t1))
                .hasMessageContaining("thu hồi");

        // Xóa First-Level Cache để ép query lấy trạng thái đã cập nhật của t2 dưới DB
        entityManager.flush();
        entityManager.clear();

        // Token t2 đang hoạt động cũng phải bị thu hồi theo chuỗi
        RefreshToken t2Entity = repo.findByTokenHash(HashUtil.sha256Hex(t2))
                .orElseThrow(() -> new AssertionError("Không tìm thấy token t2"));
        assertThat(t2Entity.isRevoked()).isTrue();
    }
}