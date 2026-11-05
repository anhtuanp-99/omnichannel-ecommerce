package com.omni.identity.user;

import com.omni.common.dto.ApiResponse;
import com.omni.identity.security.CurrentUser;
import com.omni.identity.user.dto.UpdateProfileRequest;
import com.omni.identity.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@CurrentUser UUID userId) {
        return ApiResponse.success(service.getMe(userId));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> update(
            @CurrentUser UUID userId,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ApiResponse.success(service.updateMe(userId, req));
    }
}