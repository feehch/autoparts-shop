package feehch.dev.autoparts.domain.cart;

import feehch.dev.autoparts.domain.common.Guard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Корзина покупателя. Агрегат: позиции изменяются только через методы корзины.
 */
public class Cart {

    /** Максимальное количество одного товара в корзине. */
    public static final int MAX_QUANTITY_PER_ITEM = 999;

    private final Long userId;
    private final Map<Long, Integer> items = new LinkedHashMap<>();

    public Cart(Long userId, List<CartItem> items) {
        this.userId = Guard.notNull(userId, "пользователь");
        if (items != null) {
            items.forEach(item -> this.items.merge(item.productId(), item.quantity(), Integer::sum));
        }
    }

    public static Cart empty(Long userId) {
        return new Cart(userId, List.of());
    }

    /**
     * Добавляет товар; если он уже есть в корзине — увеличивает количество.
     *
     * @return итоговое количество товара в корзине
     */
    public int add(Long productId, int quantity) {
        Guard.positive(quantity, "количество");
        int total = Math.min(quantityOf(productId) + quantity, MAX_QUANTITY_PER_ITEM);
        items.put(productId, total);
        return total;
    }

    public void setQuantity(Long productId, int quantity) {
        Guard.positive(quantity, "количество");
        items.put(productId, Math.min(quantity, MAX_QUANTITY_PER_ITEM));
    }

    public void remove(Long productId) {
        items.remove(productId);
    }

    public void clear() {
        items.clear();
    }

    public boolean contains(Long productId) {
        return items.containsKey(productId);
    }

    public int quantityOf(Long productId) {
        return items.getOrDefault(productId, 0);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<CartItem> getItems() {
        List<CartItem> result = new ArrayList<>();
        items.forEach((productId, quantity) -> result.add(new CartItem(productId, quantity)));
        return result;
    }

    public Long getUserId() {
        return userId;
    }
}
