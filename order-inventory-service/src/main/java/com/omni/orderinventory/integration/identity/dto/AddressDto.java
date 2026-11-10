package com.omni.orderinventory.integration.identity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AddressDto(
        String id,
        String recipient,
        String phone,
        String province,
        String district,
        String ward,
        String detail,
        boolean isDefault
) {}