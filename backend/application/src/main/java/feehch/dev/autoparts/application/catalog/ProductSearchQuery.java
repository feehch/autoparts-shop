package feehch.dev.autoparts.application.catalog;

import java.math.BigDecimal;

/**
 * Параметры поиска товаров, пришедшие от клиента.
 */
public record ProductSearchQuery(String q, Long categoryId, Long brandId, Long makeId, Long modelId,
                                 BigDecimal minPrice, BigDecimal maxPrice, boolean inStock,
                                 boolean includeInactive, String sort) {
}
