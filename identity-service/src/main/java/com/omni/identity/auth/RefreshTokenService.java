package com.omni.identity.auth;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.identity.user.RefreshToken;
import com.omni.identity.user.RefreshTokenRepository;
import com.omni.identity.util.HashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Refresh token — opaque random 256-bit, lưu SHA-256 hash.
 * - Rotation: dùng xong → revoke token cũ, tạo token mới.
 * - Reuse detection: nếu token đã bị revoke mà vẫn được dùng
 *   → giả định token bị lộ → revoke TOÀN BỘ refresh token của user.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final long ttlSeconds;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repo,
                               @Value("${app.jwt.refresh-token-ttl-seconds:604800}") long ttlSeconds) {
        this.repo = repo;
        this.ttlSeconds = ttlSeconds;
    }

    /** Sinh token mới + lưu hash. Trả về plaintext cho client. */
    @Transactional
    public String create(UUID userId, String userAgent, String ip) {
        String plaintext = generatePlaintext();
        RefreshToken t = new RefreshToken();
        t.setUserId(userId);
        t.setTokenHash(HashUtil.sha256Hex(plaintext));
        t.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        t.setUserAgent(userAgent);
        t.setIpAddress(ip);
        repo.save(t);
        return plaintext;
    }

    /**
     * Rotate: verify token cũ → revoke → tạo token mới.
     * Reuse detection: nếu token bị revoke được dùng lại → revoke all user tokens.
     */
    @Transactional
    public RotationResult rotate(String oldPlaintext) {
        String hash = HashUtil.sha256Hex(oldPlaintext);
        RefreshToken current = repo.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED,
                        "Refresh token không hợp lệ"));

        if (current.isRevoked()) {
            // Reuse detected — token cũ đã rotate, có thể bị lộ
            repo.revokeAllByUserId(current.getUserId());
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED,
                    "Refresh token đã bị thu hồi, vui lòng đăng nhập lại");
        }
        if (current.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "Refresh token hết hạn");
        }

        // Revoke token cũ
        current.setRevoked(true);

        // Tạo token mới
        String newPlaintext = generatePlaintext();
        RefreshToken newToken = new RefreshToken();
        newToken.setUserId(current.getUserId());
        newToken.setTokenHash(HashUtil.sha256Hex(newPlaintext));
        newToken.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        newToken.setUserAgent(current.getUserAgent());
        newToken.setIpAddress(current.getIpAddress());
        repo.save(newToken);

        current.setReplacedBy(newToken.getId());

        return new RotationResult(current.getUserId(), newPlaintext);
    }

    @Transactional
    public void revoke(String plaintext) {
        repo.findByTokenHash(HashUtil.sha256Hex(plaintext))
                .ifPresent(t -> t.setRevoked(true));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repo.revokeAllByUserId(userId);
    }

    private String generatePlaintext() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record RotationResult(UUID userId, String newRefreshToken) {}
}