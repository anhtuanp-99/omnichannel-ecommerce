package com.omni.orderinventory.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Giải mã JWT token không phụ thuộc vào identity-service (Stateless). */
@Service
public class JwtParserService {

    private static final Logger log = LoggerFactory.getLogger(JwtParserService.class);
    private final SecretKey signingKey;

    public JwtParserService(@Value("${app.jwt.secret}") String secret) {
        // Tạo khóa HMAC-SHA từ secret key dùng chung
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Trả về CurrentUser nếu token hợp lệ và còn hạn; empty nếu không hợp lệ. */
    public Optional<CurrentUser> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            if (roles == null) {
                roles = Collections.emptyList();
            }

            return Optional.of(new CurrentUser(userId, email, roles));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}