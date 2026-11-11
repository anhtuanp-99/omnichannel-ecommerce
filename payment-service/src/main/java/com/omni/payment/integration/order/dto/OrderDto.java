package com.omni.payment.integration.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderDto(UUID id, String status, long totalAmount, String currency) {}