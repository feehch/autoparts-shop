package feehch.dev.autoparts.application.catalog;

import java.math.BigDecimal;
import java.util.Set;

public record ProductCommand(String sku, String oemNumber, String name, String description, BigDecimal price,
                             int stockQuantity, String imageUrl, Long brandId, Long categoryId,
                             Set<Long> compatibleModelIds, boolean active) {
}
