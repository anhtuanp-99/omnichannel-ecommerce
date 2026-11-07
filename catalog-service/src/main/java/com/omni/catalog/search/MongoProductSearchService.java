package com.omni.catalog.search;

import com.omni.catalog.product.Product;
import com.omni.catalog.product.Sku;
import com.omni.catalog.product.dto.ProductSummaryResponse;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class MongoProductSearchService implements ProductSearchService {

    private final MongoTemplate mongo;

    public MongoProductSearchService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public SearchResult search(ProductSearchQuery q) {
        int page = q.safePage();
        int size = q.safeSize();

        Query query = new Query();

        // 1. Điều kiện bắt buộc: status ACTIVE
        query.addCriteria(Criteria.where("status").is("ACTIVE"));

        // 2. Full-text search mô phỏng qua Regex tiếng Việt
        if (q.keyword() != null && !q.keyword().isBlank()) {
            String regexPattern = buildVietnameseRegex(q.keyword().trim());
            Pattern p = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(p),
                    Criteria.where("description").regex(p),
                    Criteria.where("skus.code").regex(p)
            ));
        }

        // 3. Lọc theo categoryId
        if (q.categoryId() != null && !q.categoryId().isBlank()) {
            query.addCriteria(Criteria.where("categoryId").is(q.categoryId()));
        }

        // 4. Lọc theo brandId
        if (q.brandId() != null && !q.brandId().isBlank()) {
            query.addCriteria(Criteria.where("brandId").is(q.brandId()));
        }

        // 5. Sắp xếp
        Sort sort = switch (q.sort() == null ? "relevance" : q.sort()) {
            case "created_desc" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "updated_desc" -> Sort.by(Sort.Direction.DESC, "updatedAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        query.with(sort);

        // 6. Đếm tổng số bản ghi khớp
        long total = mongo.count(query, Product.class);

        // 7. Phân trang
        query.skip((long) page * size).limit(size);
        List<Product> products = mongo.find(query, Product.class);

        // 8. Lọc giá trên memory (do SKU nhúng)
        List<ProductSummaryResponse> items = products.stream()
                .filter(p -> priceMatch(p, q))
                .map(this::toSummary)
                .toList();

        return new SearchResult(items, total, page, size, "mongo");
    }

    private boolean priceMatch(Product p, ProductSearchQuery q) {
        long min = p.getSkus().stream().mapToLong(Sku::getPrice).min().orElse(0);
        long max = p.getSkus().stream().mapToLong(Sku::getPrice).max().orElse(0);
        if (q.priceMin() != null && max < q.priceMin()) return false;
        if (q.priceMax() != null && min > q.priceMax()) return false;
        return true;
    }

    private ProductSummaryResponse toSummary(Product p) {
        long min = p.getSkus().stream().mapToLong(Sku::getPrice).min().orElse(0);
        long max = p.getSkus().stream().mapToLong(Sku::getPrice).max().orElse(0);
        String currency = p.getSkus().isEmpty() ? "VND" : p.getSkus().get(0).getCurrency();
        String primary = p.getImages().stream()
                .filter(Product.Image::isPrimary)
                .map(Product.Image::url)
                .findFirst()
                .orElse(null);
        List<String> codes = p.getSkus().stream().map(Sku::getCode).toList();
        return new ProductSummaryResponse(p.getId(), p.getName(), p.getCategoryId(), p.getBrandId(),
                min, max, currency, primary, codes);
    }

    private String buildVietnameseRegex(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toLowerCase().toCharArray()) {
            switch (c) {
                case 'a' -> sb.append("[aàáảãạăằắẳẵặâầấẩẫậ]");
                case 'e' -> sb.append("[eèéẻẽẹêềếểễệ]");
                case 'i' -> sb.append("[iìíỉĩị]");
                case 'o' -> sb.append("[oòóỏõọôồốổỗộơờớởỡợ]");
                case 'u' -> sb.append("[uùúủũụưừứửữự]");
                case 'y' -> sb.append("[yỳýỷỹỵ]");
                case 'd' -> sb.append("[dđ]");
                default -> {
                    if (Character.isLetterOrDigit(c)) {
                        sb.append(c);
                    } else {
                        sb.append(Pattern.quote(String.valueOf(c)));
                    }
                }
            }
        }
        return sb.toString();
    }
}