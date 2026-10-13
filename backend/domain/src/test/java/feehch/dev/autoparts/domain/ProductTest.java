package feehch.dev.autoparts.domain;

import feehch.dev.autoparts.domain.catalog.InsufficientStockException;
import feehch.dev.autoparts.domain.catalog.Product;
import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.common.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");

    private Product product(int stock) {
        return Product.create("bos-123", null, "Колодки", null, new BigDecimal("999.999"), stock, null, true,
                1L, 2L, Set.of(5L), NOW);
    }

    @Test
    void normalizesSkuAndPrice() {
        Product p = product(5);

        assertThat(p.getSku()).isEqualTo("BOS-123");
        assertThat(p.getPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void reserveDecreasesStock() {
        Product p = product(5);
        p.reserve(3, NOW);

        assertThat(p.getStockQuantity()).isEqualTo(2);
    }

    @Test
    void cannotReserveMoreThanInStock() {
        Product p = product(2);

        assertThatThrownBy(() -> p.reserve(3, NOW)).isInstanceOf(InsufficientStockException.class);
        assertThat(p.getStockQuantity()).isEqualTo(2);
    }

    @Test
    void inactiveProductCannotBeOrdered() {
        Product p = product(10);
        p.deactivate(NOW);

        assertThatThrownBy(() -> p.ensureCanBeOrdered(1)).isInstanceOf(ConflictException.class);
        assertThat(p.isAvailable(1)).isFalse();
    }

    @Test
    void releaseReturnsStock() {
        Product p = product(1);
        p.release(4, NOW);

        assertThat(p.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void negativePriceIsRejected() {
        assertThatThrownBy(() -> Product.create("A", null, "B", null, new BigDecimal("-1"), 0, null, true,
                1L, 1L, Set.of(), NOW)).isInstanceOf(DomainException.class);
    }
}
