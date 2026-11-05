package com.omni.identity.admin.dto;
import jakarta.validation.constraints.NotBlank;
public record AssignRoleRequest(@NotBlank String roleCode) {}