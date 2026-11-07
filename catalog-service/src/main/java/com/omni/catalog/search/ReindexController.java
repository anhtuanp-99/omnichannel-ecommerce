package com.omni.catalog.search;

import com.omni.catalog.search.dto.SearchResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/catalog")
public class ReindexController {

    private final ReindexService reindexService;
    private final ProductSearchService searchService;

    public ReindexController(ReindexService reindexService, ProductSearchService searchService) {
        this.reindexService = reindexService;
        this.searchService = searchService;
    }

    @PostMapping("/admin/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> reindex() {
        long count = reindexService.reindexAll();
        return Map.of("reindexed", count, "message", "ES index rebuilt");
    }

    @GetMapping("/products/search")
    public SearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) Long priceMin,
            @RequestParam(required = false) Long priceMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sort) {
        var query = new ProductSearchQuery(q, categoryId, brandId, priceMin, priceMax, page, size, sort);
        return SearchResponse.from(searchService.search(query));
    }
}