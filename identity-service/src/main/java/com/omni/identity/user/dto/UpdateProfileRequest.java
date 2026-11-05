package com.omni.identity.user.dto;
import jakarta.validation.constraints.Size;
public record UpdateProfileRequest(
        @Size(max = 100) String fullName,
        @Size(max = 20) String phone
) {}