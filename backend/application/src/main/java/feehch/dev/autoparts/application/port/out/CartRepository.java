package feehch.dev.autoparts.application.port.out;

import feehch.dev.autoparts.domain.cart.Cart;

/**
 * Порт хранилища корзин.
 */
public interface CartRepository {

    /**
     * Корзина пользователя; если её нет — пустая корзина.
     */
    Cart findByUserId(Long userId);

    void save(Cart cart);
}
