package com.omni.catalog.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.omni.catalog.product.dto.ProductSummaryResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.search.use-elasticsearch", havingValue = "true", matchIfMissing = true)
public class ElasticProductSearchService implements ProductSearchService {

    private final ElasticsearchOperations ops;

    public ElasticProductSearchService(ElasticsearchOperations ops) {
        this.ops = ops;
    }

    @Override
    public SearchResult search(ProductSearchQuery q) {
        int page = q.safePage();
        int size = q.safeSize();

        // 1. bool.must (relevance) — match name/description/skuCodes
        List<Query> must = new ArrayList<>();
        if (q.keyword() != null && !q.keyword().isBlank()) {
            must.add(Query.of(b -> b.multiMatch(m -> m
                    .query(q.keyword())
                    .fields("name^3", "description", "skuCodes^2")
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
            )));
        } else {
            must.add(Query.of(b -> b.matchAll(m -> m)));
        }

        // 2. bool.filter
        List<Query> filters = new ArrayList<>();
        filters.add(Query.of(b -> b.term(t -> t.field("status").value("ACTIVE"))));
        if (q.categoryId() != null && !q.categoryId().isBlank()) {
            filters.add(Query.of(b -> b.term(t -> t.field("categoryId").value(q.categoryId()))));
        }
        if (q.brandId() != null && !q.brandId().isBlank()) {
            filters.add(Query.of(b -> b.term(t -> t.field("brandId").value(q.brandId()))));
        }
        // Price range overlap: minPrice <= userMax AND maxPrice >= userMin
        if (q.priceMin() != null) {
            filters.add(Query.of(b -> b.range(r -> r.number(n -> n.field("maxPrice").gte(q.priceMin().doubleValue())))));
        }
        if (q.priceMax() != null) {
            filters.add(Query.of(b -> b.range(r -> r.number(n -> n.field("minPrice").lte(q.priceMax().doubleValue())))));
        }

        Query finalQuery = Query.of(b -> b.bool(bb -> bb.must(must).filter(filters)));

        // 3. Xử lý Sorting qua Spring Data Domain Sort
        Sort sort = switch (q.sort() == null ? "relevance" : q.sort()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "minPrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "minPrice");
            case "created_desc" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "updated_desc" -> Sort.by(Sort.Direction.DESC, "updatedAt");
            default -> Sort.unsorted();
        };

        NativeQuery query = NativeQuery.builder()
                .withQuery(finalQuery)
                .withPageable(PageRequest.of(page, size, sort))
                .build();

        SearchHits<ProductSearchDocument> hits = ops.search(query, ProductSearchDocument.class);
        List<ProductSummaryResponse> items = hits.getSearchHits().stream()
                .map(h -> toSummary(h.getContent()))
                .toList();

        return new SearchResult(items, hits.getTotalHits(), page, size, "elasticsearch");
    }

    private ProductSummaryResponse toSummary(ProductSearchDocument d) {
        return new ProductSummaryResponse(
                d.getId(), d.getName(), d.getCategoryId(), d.getBrandId(),
                d.getMinPrice(), d.getMaxPrice(), d.getCurrency(),
                d.getPrimaryImageUrl(),
                d.getSkuCodes() == null ? List.of() : d.getSkuCodes());
    }
}