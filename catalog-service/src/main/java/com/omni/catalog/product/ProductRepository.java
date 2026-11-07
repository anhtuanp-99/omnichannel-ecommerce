package com.omni.catalog.product;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByStatusOrderByCreatedAtDesc(String status);
    List<Product> findByCategoryIdAndStatus(String categoryId, String status);
}