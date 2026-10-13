package feehch.dev.autoparts.domain;

import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.common.DomainException;
import feehch.dev.autoparts.domain.order.InvalidOrderStatusTransitionException;
import feehch.dev.autoparts.domain.order.Order;
import feehch.dev.autoparts.domain.order.OrderItem;
import feehch.dev.autoparts.domain.order.OrderStatus;
import feehch.dev.autoparts.domain.order.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");

    private Order newOrder() {
        return Order.place(1L, List.of(
                        new OrderItem(10L, "SKU-1", "Колодки", new BigDecimal("1000.50"), 2),
                        new OrderItem(11L, "SKU-2", "Фильтр", new BigDecimal("300.00"), 1)),
                "Москва", "+375291234567", null, PaymentMethod.CASH_ON_DELIVERY, NOW);
    }

    @Test
    void placeCalculatesTotalAndStartsAsNew() {
        Order order = newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("2301.00");
    }

    @Test
    void orderWithoutItemsIsRejected() {
        assertThatThrownBy(() -> Order.place(1L, List.of(), "Москва", "+375291234567", null,
                PaymentMethod.CARD_ONLINE, NOW)).isInstanceOf(DomainException.class);
    }

    @ParameterizedTest
    @CsvSource({"NEW,CONFIRMED", "NEW,CANCELLED", "CONFIRMED,SHIPPED", "CONFIRMED,CANCELLED", "SHIPPED,DELIVERED"})
    void allowedTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"NEW,SHIPPED", "NEW,DELIVERED", "SHIPPED,CANCELLED", "DELIVERED,CANCELLED", "CANCELLED,NEW"})
    void forbiddenTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void fullLifecycle() {
        Order order = newOrder();
        order.changeStatus(OrderStatus.CONFIRMED, NOW);
        order.changeStatus(OrderStatus.SHIPPED, NOW);
        order.changeStatus(OrderStatus.DELIVERED, NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThatThrownBy(() -> order.changeStatus(OrderStatus.CANCELLED, NOW))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void customerCanCancelOnlyNewOrder() {
        Order order = newOrder();
        order.changeStatus(OrderStatus.CONFIRMED, NOW);

        assertThatThrownBy(() -> order.cancelByCustomer(NOW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void numberIsFormatted() {
        Order order = newOrder();
        order.setId(7L);

        assertThat(order.number()).isEqualTo("AP-000007");
    }
}
