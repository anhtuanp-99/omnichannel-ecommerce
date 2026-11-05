package com.omni.identity.user;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.identity.user.dto.UpdateProfileRequest;
import com.omni.identity.user.dto.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) { this.repo = repo; }

    public UserProfileResponse getMe(UUID userId) {
        User u = repo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResponse(u);
    }

    @Transactional
    public UserProfileResponse updateMe(UUID userId, UpdateProfileRequest req) {
        User u = repo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (req.fullName() != null && !req.fullName().isBlank()) u.setFullName(req.fullName());
        if (req.phone() != null) u.setPhone(req.phone());
        return toResponse(u);
    }

    private UserProfileResponse toResponse(User u) {
        return new UserProfileResponse(
                u.getId().toString(), u.getEmail(), u.getPhone(), u.getFullName(),
                u.getStatus().name(), u.isEmailVerified(),
                u.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()),
                u.getCreatedAt());
    }
}