package com.omni.catalog.brand;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface BrandRepository extends MongoRepository<Brand, String> {
    Optional<Brand> findBySlug(String slug);
    boolean existsBySlug(String slug);
}