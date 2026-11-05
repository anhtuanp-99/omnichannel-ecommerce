package com.omni.identity.user.dto;
import java.time.Instant;
import java.util.Set;
public record UserProfileResponse(
        String id, String email, String phone, String fullName,
        String status, boolean emailVerified,
        Set<String> roles, Instant createdAt
) {}