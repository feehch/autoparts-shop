package feehch.dev.autoparts.domain.cart;

import feehch.dev.autoparts.domain.common.Guard;

/**
 * Позиция корзины.
 */
public record CartItem(Long productId, int quantity) {

    public CartItem {
        Guard.notNull(productId, "товар");
        Guard.positive(quantity, "количество");
    }
}
