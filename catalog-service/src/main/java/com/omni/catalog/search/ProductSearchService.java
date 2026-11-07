package com.omni.catalog.search;

public interface ProductSearchService {
    SearchResult search(ProductSearchQuery query);
}