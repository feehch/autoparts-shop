package feehch.dev.autoparts.application.cart;

import java.math.BigDecimal;

/**
 * Позиция корзины с актуальными данными товара.
 *
 * @param available {@code false}, если товар деактивирован или его остатка не хватает
 */
public record CartItemView(Long productId, String sku, String name, String imageUrl, BigDecimal price, int quantity,
                           BigDecimal lineTotal, int stockQuantity, boolean available) {
}
