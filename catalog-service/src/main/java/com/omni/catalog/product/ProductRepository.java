package com.omni.catalog.product;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {

    /** Lấy danh sách sản phẩm theo trạng thái, mới nhất xếp trước */
    List<Product> findByStatusOrderByCreatedAtDesc(String status);

    /** Lấy danh sách sản phẩm theo danh mục và trạng thái */
    List<Product> findByCategoryIdAndStatus(String categoryId, String status);

    /** Tìm Product chứa phần tử SKU có id tương ứng (tránh findAll full-scan) */
    @Query("{ 'skus._id': ?0 }")
    Optional<Product> findBySkuId(String skuId);

}