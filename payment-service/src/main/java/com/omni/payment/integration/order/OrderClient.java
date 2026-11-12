package com.omni.payment.integration.order;

import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import com.omni.payment.integration.order.dto.OrderDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-inventory-service", path = "/api/v1")
public interface OrderClient {

    @GetMapping("/orders/{id}")
    @CircuitBreaker(name = "orderClient", fallbackMethod = "getOrderFallback")
    OrderDto getOrder(@PathVariable("id") UUID id);

    default OrderDto getOrderFallback(UUID id, Throwable t) {
        throw new BusinessException(ErrorCode.UPSTREAM_UNAVAILABLE, "Order service tạm thời không khả dụng");
    }
}