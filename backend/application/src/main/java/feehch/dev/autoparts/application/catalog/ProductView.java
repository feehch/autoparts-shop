package feehch.dev.autoparts.application.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductView(Long id, String sku, String oemNumber, String name, BigDecimal price, int stockQuantity,
                          String imageUrl, boolean active, Long brandId, String brandName, Long categoryId,
                          String categoryName, String description, List<CarModelView> compatibleModels,
                          Instant createdAt, Instant updatedAt) {
}
