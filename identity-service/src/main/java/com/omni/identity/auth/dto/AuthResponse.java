package com.omni.identity.auth.dto;
import java.util.Set;
public record AuthResponse(
        String userId,
        String email,
        String fullName,
        Set<String> roles,
        TokenPair tokens
) {}