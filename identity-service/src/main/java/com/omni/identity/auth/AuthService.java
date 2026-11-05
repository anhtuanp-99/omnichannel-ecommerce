package com.omni.identity.auth;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.identity.audit.AuditService;
import com.omni.identity.auth.dto.*;
import com.omni.identity.user.*;
import com.omni.identity.util.HashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final VerificationTokenRepository verificationRepo;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    private final long verifyTtlSeconds;
    private final long resetTtlSeconds;
    private final int maxFailedAttempts;
    private final long lockDurationSeconds;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepo, RoleRepository roleRepo,
                       VerificationTokenRepository verificationRepo,
                       RefreshTokenService refreshTokenService, JwtService jwtService,
                       PasswordEncoder passwordEncoder, EmailService emailService,
                       AuditService auditService,
                       @Value("${app.security.verify-ttl-seconds:86400}") long verifyTtlSeconds,
                       @Value("${app.security.reset-ttl-seconds:3600}") long resetTtlSeconds,
                       @Value("${app.security.max-failed-attempts:5}") int maxFailedAttempts,
                       @Value("${app.security.lock-duration-seconds:900}") long lockDurationSeconds) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.verificationRepo = verificationRepo;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditService = auditService;
        this.verifyTtlSeconds = verifyTtlSeconds;
        this.resetTtlSeconds = resetTtlSeconds;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDurationSeconds = lockDurationSeconds;
    }

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepo.existsByEmailAndDeletedAtIsNull(req.email().toLowerCase())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Email đã được đăng ký");
        }

        User user = new User();
        user.setEmail(req.email().toLowerCase());
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName());
        user.setStatus(User.Status.PENDING_VERIFY);
        user.setEmailVerified(false);

        Role customer = roleRepo.findByCode("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Role CUSTOMER chưa được seed trong database"));
        user.getRoles().add(customer);

        userRepo.save(user);

        String token = generateToken();
        VerificationToken vt = new VerificationToken();
        vt.setUserId(user.getId());
        vt.setTokenHash(HashUtil.sha256Hex(token));
        vt.setPurpose(VerificationToken.Purpose.VERIFY_EMAIL);
        vt.setExpiresAt(Instant.now().plusSeconds(verifyTtlSeconds));
        verificationRepo.save(vt);

        emailService.sendVerifyEmail(user.getEmail(), token);
        auditService.log(user.getId(), "AUTH_REGISTER", "User", user.getId().toString(), null, "SUCCESS");
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken vt = verificationRepo
                .findByTokenHashAndPurpose(HashUtil.sha256Hex(token), VerificationToken.Purpose.VERIFY_EMAIL)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Token không hợp lệ"));
        if (vt.isUsed()) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Token đã được dùng");
        if (vt.getExpiresAt().isBefore(Instant.now())) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Token hết hạn");

        User user = userRepo.findById(vt.getUserId()).orElseThrow();
        user.setEmailVerified(true);
        user.setStatus(User.Status.ACTIVE);
        vt.setUsed(true);
        auditService.log(user.getId(), "AUTH_VERIFY_EMAIL", "User", user.getId().toString(), null, "SUCCESS");
    }

    @Transactional
    public AuthResponse login(LoginRequest req, String userAgent, String ip) {
        User user = userRepo.findByEmailAndDeletedAtIsNull(req.email().toLowerCase()).orElse(null);

        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            if (user != null) {
                handleFailedLogin(user);
            }
            auditService.log(user != null ? user.getId() : null,
                    "AUTH_LOGIN_FAILED", "User", req.email(), null, "FAILURE");
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "Email hoặc mật khẩu không đúng");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "Tài khoản đang bị tạm khóa");
        }
        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Vui lòng xác minh email");
        }
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "Tài khoản không hoạt động");
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);

        String access = jwtService.generateAccessToken(user);
        String refresh = refreshTokenService.create(user.getId(), userAgent, ip);

        auditService.log(user.getId(), "AUTH_LOGIN_SUCCESS", "User", user.getId().toString(), null, "SUCCESS");

        Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        return new AuthResponse(user.getId().toString(), user.getEmail(), user.getFullName(),
                roles, new TokenPair(access, refresh, 900, 604800));
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req, String userAgent, String ip) {
        var rotation = refreshTokenService.rotate(req.refreshToken());
        User user = userRepo.findById(rotation.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "User không tồn tại"));
        String access = jwtService.generateAccessToken(user);
        Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        return new AuthResponse(user.getId().toString(), user.getEmail(), user.getFullName(),
                roles, new TokenPair(access, rotation.newRefreshToken(), 900, 604800));
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepo.findByEmailAndDeletedAtIsNull(req.email().toLowerCase()).ifPresent(user -> {
            String token = generateToken();
            VerificationToken vt = new VerificationToken();
            vt.setUserId(user.getId());
            vt.setTokenHash(HashUtil.sha256Hex(token));
            vt.setPurpose(VerificationToken.Purpose.RESET_PASSWORD);
            vt.setExpiresAt(Instant.now().plusSeconds(resetTtlSeconds));
            verificationRepo.save(vt);
            emailService.sendResetPassword(user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        VerificationToken vt = verificationRepo
                .findByTokenHashAndPurpose(HashUtil.sha256Hex(req.token()), VerificationToken.Purpose.RESET_PASSWORD)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Token không hợp lệ"));
        if (vt.isUsed()) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Token đã dùng");
        if (vt.getExpiresAt().isBefore(Instant.now())) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Token hết hạn");

        User user = userRepo.findById(vt.getUserId()).orElseThrow();
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        vt.setUsed(true);

        refreshTokenService.revokeAllForUser(user.getId());
        auditService.log(user.getId(), "AUTH_RESET_PASSWORD", "User", user.getId().toString(), null, "SUCCESS");
    }

    private void handleFailedLogin(User user) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        if (user.getFailedLoginCount() >= maxFailedAttempts) {
            user.setLockedUntil(Instant.now().plusSeconds(lockDurationSeconds));
            user.setFailedLoginCount(0);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}