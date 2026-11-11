package com.omni.payment.integration.order;

import com.omni.payment.integration.order.dto.OrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-inventory-service", path = "/api/v1")
public interface OrderClient {

    @GetMapping("/orders/{id}")
    OrderDto getOrder(@PathVariable("id") UUID id);
}