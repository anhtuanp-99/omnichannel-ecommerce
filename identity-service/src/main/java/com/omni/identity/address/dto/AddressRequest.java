package com.omni.identity.address.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AddressRequest(
        @NotBlank @Size(max = 100) String recipient,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 100) String province,
        @NotBlank @Size(max = 100) String district,
        @NotBlank @Size(max = 100) String ward,
        @NotBlank @Size(max = 255) String detail,
        boolean isDefault
) {}