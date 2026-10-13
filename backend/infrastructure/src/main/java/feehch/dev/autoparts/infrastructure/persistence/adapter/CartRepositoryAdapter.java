package feehch.dev.autoparts.infrastructure.persistence.adapter;

import feehch.dev.autoparts.application.port.out.CartRepository;
import feehch.dev.autoparts.domain.cart.Cart;
import feehch.dev.autoparts.domain.cart.CartItem;
import feehch.dev.autoparts.infrastructure.persistence.entity.CartItemEntity;
import feehch.dev.autoparts.infrastructure.persistence.jpa.CartItemJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class CartRepositoryAdapter implements CartRepository {

    private final CartItemJpaRepository jpa;
    private final Clock clock;

    public CartRepositoryAdapter(CartItemJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public Cart findByUserId(Long userId) {
        List<CartItem> items = jpa.findByUserId(userId).stream()
                .map(e -> new CartItem(e.getId().getProductId(), e.getQuantity()))
                .toList();
        return new Cart(userId, items);
    }

    /**
     * Синхронизирует позиции корзины в БД с состоянием агрегата, сохраняя дату добавления
     * уже существующих позиций (по ней упорядочивается корзина).
     */
    @Override
    @Transactional
    public void save(Cart cart) {
        Map<Long, Instant> addedAt = jpa.findByUserId(cart.getUserId()).stream()
                .collect(Collectors.toMap(e -> e.getId().getProductId(), CartItemEntity::getAddedAt));
        jpa.deleteByUserId(cart.getUserId());
        Instant now = clock.instant();
        List<CartItemEntity> entities = cart.getItems().stream()
                .map(item -> new CartItemEntity(cart.getUserId(), item.productId(), item.quantity(),
                        addedAt.getOrDefault(item.productId(), now)))
                .toList();
        jpa.saveAll(entities);
    }
}
