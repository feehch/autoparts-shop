package feehch.dev.autoparts.application.catalog;

import java.math.BigDecimal;

public record ProductSummaryView(Long id, String sku, String oemNumber, String name, BigDecimal price,
                                 int stockQuantity, String imageUrl, boolean active, Long brandId, String brandName,
                                 Long categoryId, String categoryName) {
}
