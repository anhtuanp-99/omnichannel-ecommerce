package com.omni.identity.admin;

import com.omni.common.dto.ApiResponse;
import com.omni.identity.admin.dto.AssignRoleRequest;
import com.omni.identity.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class RoleAdminController {

    private final RoleAdminService service;

    public RoleAdminController(RoleAdminService service) {
        this.service = service;
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> assign(
            @CurrentUser UUID actorId,
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequest req) {
        service.assign(actorId, userId, req.roleCode());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{userId}/roles/{roleCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> revoke(
            @CurrentUser UUID actorId,
            @PathVariable UUID userId,
            @PathVariable String roleCode) {
        service.revoke(actorId, userId, roleCode);
        return ApiResponse.success(null);
    }
}