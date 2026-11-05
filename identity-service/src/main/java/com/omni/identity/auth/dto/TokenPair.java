package com.omni.identity.auth.dto;
public record TokenPair(
        String accessToken,
        String refreshToken,
        long accessExpiresInSeconds,
        long refreshExpiresInSeconds
) {}