package com.omni.identity.admin;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.identity.audit.AuditService;
import com.omni.identity.user.Role;
import com.omni.identity.user.RoleRepository;
import com.omni.identity.user.User;
import com.omni.identity.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class RoleAdminService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final AuditService auditService;

    public RoleAdminService(UserRepository userRepo, RoleRepository roleRepo,
                            AuditService auditService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.auditService = auditService;
    }

    @Transactional
    public void assign(UUID actorId, UUID targetUserId, String roleCode) {
        User user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Role role = roleRepo.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        user.getRoles().add(role);
        auditService.log(actorId, "ADMIN_ASSIGN_ROLE", "User", user.getId().toString(),
                Map.of("roleCode", roleCode), "SUCCESS");
    }

    @Transactional
    public void revoke(UUID actorId, UUID targetUserId, String roleCode) {
        User user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Role role = roleRepo.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        user.getRoles().remove(role);
        auditService.log(actorId, "ADMIN_REVOKE_ROLE", "User", user.getId().toString(),
                Map.of("roleCode", roleCode), "SUCCESS");
    }
}