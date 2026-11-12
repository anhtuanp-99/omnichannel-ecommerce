package com.omni.notification.integration.identity;

import com.omni.notification.integration.identity.dto.UserContactDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service", path = "/api/v1/internal/users")
public interface IdentityClient {

    @GetMapping("/{id}/contact")
    @CircuitBreaker(name = "identityClient", fallbackMethod = "getContactFallback")
    UserContactDto getContact(@PathVariable("id") String id);

    default UserContactDto getContactFallback(String id, Throwable t) {
        throw new RuntimeException("Identity service không phản hồi — để Kafka tự động retry sau", t);
    }
}