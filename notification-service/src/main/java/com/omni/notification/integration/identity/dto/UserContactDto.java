package com.omni.notification.integration.identity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserContactDto(String id, String email, String fullName) {}