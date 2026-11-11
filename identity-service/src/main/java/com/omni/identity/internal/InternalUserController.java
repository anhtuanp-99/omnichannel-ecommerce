package com.omni.identity.internal;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.identity.user.User;
import com.omni.identity.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Endpoint nội bộ giữa các microservices (không public ra ngoài Gateway). */
@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final UserRepository userRepo;

    public InternalUserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/{id}/contact")
    public ResponseEntity<UserContactResponse> getContact(@PathVariable UUID id) {
        User u = userRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));

        UserContactResponse response = new UserContactResponse(
                u.getId().toString(),
                u.getEmail(),
                u.getFullName()
        );

        return ResponseEntity.ok(response);
    }

    /** DTO trả về thông tin liên hệ cơ bản của User. */
    public record UserContactResponse(
            String id,
            String email,
            String fullName
    ) {}
}