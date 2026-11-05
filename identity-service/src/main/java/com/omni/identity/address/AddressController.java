package com.omni.identity.address;

import com.omni.common.dto.ApiResponse;
import com.omni.identity.address.dto.AddressRequest;
import com.omni.identity.address.dto.AddressResponse;
import com.omni.identity.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list(@CurrentUser UUID userId) {
        return ApiResponse.success(service.list(userId));
    }

    @PostMapping
    public ApiResponse<AddressResponse> create(
            @CurrentUser UUID userId,
            @Valid @RequestBody AddressRequest req) {
        return ApiResponse.success(service.create(userId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> update(
            @CurrentUser UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest req) {
        return ApiResponse.success(service.update(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @CurrentUser UUID userId,
            @PathVariable UUID id) {
        service.delete(userId, id);
        return ApiResponse.success(null);
    }
}