package com.omni.notification.integration.identity;

import com.omni.notification.integration.identity.dto.UserContactDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service", path = "/api/v1/internal/users")
public interface IdentityClient {

    @GetMapping("/{id}/contact")
    UserContactDto getContact(@PathVariable("id") String id);
}