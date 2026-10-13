package feehch.dev.autoparts.domain.order;

import feehch.dev.autoparts.domain.common.Guard;

import java.math.BigDecimal;

/**
 * Позиция заказа. Хранит «снимок» товара на момент оформления (артикул, название, цену),
 * чтобы последующие изменения каталога не влияли на уже оформленные заказы.
 */
public record OrderItem(Long productId, String sku, String name, BigDecimal price, int quantity) {

    public OrderItem {
        Guard.notNull(productId, "товар");
        Guard.notBlank(sku, "артикул");
        Guard.notBlank(name, "название");
        Guard.notNegative(price, "цена");
        Guard.positive(quantity, "количество");
    }

    public BigDecimal lineTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
