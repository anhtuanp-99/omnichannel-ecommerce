package com.omni.catalog.category;

import com.omni.catalog.category.dto.CategoryRequest;
import com.omni.catalog.category.dto.CategoryResponse;
import com.omni.common.error.BusinessException;
import com.omni.common.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) { this.repo = repo; }

    public List<CategoryResponse> listAll() {
        return repo.findAll().stream().map(this::toResp).toList();
    }

    public CategoryResponse getById(String id) {
        return repo.findById(id).map(this::toResp)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public CategoryResponse create(CategoryRequest req) {
        if (repo.existsBySlug(req.slug())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Slug đã tồn tại");
        }
        // Chặn cycle: parent không được là chính nó (khi update) — create thì chưa có id
        if (req.parentId() != null && !repo.existsById(req.parentId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Parent không tồn tại");
        }
        Category c = new Category();
        apply(c, req);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(c.getCreatedAt());
        return toResp(repo.save(c));
    }

    public CategoryResponse update(String id, CategoryRequest req) {
        Category c = repo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        // Chặn cycle đơn giản: không cho parent trùng chính nó
        if (id.equals(req.parentId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Category không thể là parent của chính nó");
        }
        apply(c, req);
        c.setUpdatedAt(Instant.now());
        return toResp(repo.save(c));
    }

    public void delete(String id) {
        // Soft delete: đổi status INACTIVE nếu còn product tham chiếu — đơn giản: check children
        if (repo.existsByParentId(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Không xóa danh mục còn danh mục con");
        }
        repo.deleteById(id);
    }

    private void apply(Category c, CategoryRequest req) {
        c.setName(req.name());
        c.setSlug(req.slug());
        c.setParentId(req.parentId());
        c.setStatus(req.status());
    }

    private CategoryResponse toResp(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getParentId(),
                c.getStatus(), c.getCreatedAt(), c.getUpdatedAt());
    }
}