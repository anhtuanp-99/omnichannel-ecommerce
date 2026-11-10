package com.omni.orderinventory.cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    // Sử dụng @EntityGraph để tải luôn danh sách items, tránh lỗi LazyInitializationException
    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByCustomerId(UUID customerId);
}