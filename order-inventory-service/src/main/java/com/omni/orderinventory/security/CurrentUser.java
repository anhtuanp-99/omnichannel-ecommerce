package com.omni.orderinventory.security;

import java.util.List;
import java.util.UUID;

/** Snapshot danh tính người dùng từ claims của JWT. */
public record CurrentUser(
        UUID userId,
        String email,
        List<String> roles
) {
    /** Kiểm tra nhanh vai trò quyền hạn. */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}