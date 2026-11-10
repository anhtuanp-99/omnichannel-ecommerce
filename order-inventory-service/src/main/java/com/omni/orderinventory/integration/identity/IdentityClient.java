package com.omni.orderinventory.integration.identity;

import com.omni.common.dto.ApiResponse;
import com.omni.orderinventory.integration.identity.dto.AddressDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Feign client kết nối Identity-service qua Eureka LoadBalancer. */
@FeignClient(name = "identity-service", path = "/api/v1/addresses")
public interface IdentityClient {

    /** Lấy chi tiết địa chỉ (Dùng khi Checkout) */
    @GetMapping("/{id}")
    ApiResponse<AddressDto> getAddress(@PathVariable("id") UUID id);
}