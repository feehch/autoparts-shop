package feehch.dev.autoparts.domain;

import feehch.dev.autoparts.domain.cart.Cart;
import feehch.dev.autoparts.domain.common.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    @Test
    void addingSameProductAccumulatesQuantity() {
        Cart cart = Cart.empty(1L);
        cart.add(10L, 2);
        cart.add(10L, 3);

        assertThat(cart.quantityOf(10L)).isEqualTo(5);
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void quantityIsCapped() {
        Cart cart = Cart.empty(1L);
        cart.add(10L, 990);
        cart.add(10L, 20);

        assertThat(cart.quantityOf(10L)).isEqualTo(Cart.MAX_QUANTITY_PER_ITEM);
    }

    @Test
    void removeAndClear() {
        Cart cart = Cart.empty(1L);
        cart.add(10L, 1);
        cart.add(11L, 1);
        cart.remove(10L);

        assertThat(cart.contains(10L)).isFalse();
        cart.clear();
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void zeroQuantityIsRejected() {
        assertThatThrownBy(() -> Cart.empty(1L).setQuantity(10L, 0)).isInstanceOf(DomainException.class);
    }
}
