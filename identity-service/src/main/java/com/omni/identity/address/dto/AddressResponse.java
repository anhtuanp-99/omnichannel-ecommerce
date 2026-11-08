package com.omni.identity.address.dto;

public record AddressResponse(
        String id,
        String recipient,
        String phone,
        String province,
        String district,
        String ward,
        String detail,
        boolean isDefault
) {}