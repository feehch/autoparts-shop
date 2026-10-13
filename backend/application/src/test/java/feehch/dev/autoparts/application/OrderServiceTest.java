package feehch.dev.autoparts.application;

import feehch.dev.autoparts.application.order.OrderService;
import feehch.dev.autoparts.application.order.OrderView;
import feehch.dev.autoparts.application.order.PlaceOrderCommand;
import feehch.dev.autoparts.application.port.out.CartRepository;
import feehch.dev.autoparts.application.port.out.OrderRepository;
import feehch.dev.autoparts.application.port.out.ProductRepository;
import feehch.dev.autoparts.application.port.out.UserRepository;
import feehch.dev.autoparts.application.security.AccessDeniedException;
import feehch.dev.autoparts.application.security.AuthenticatedUser;
import feehch.dev.autoparts.domain.cart.Cart;
import feehch.dev.autoparts.domain.catalog.InsufficientStockException;
import feehch.dev.autoparts.domain.catalog.Product;
import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.order.Order;
import feehch.dev.autoparts.domain.order.OrderItem;
import feehch.dev.autoparts.domain.order.OrderStatus;
import feehch.dev.autoparts.domain.order.PaymentMethod;
import feehch.dev.autoparts.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");
    private static final PlaceOrderCommand COMMAND =
            new PlaceOrderCommand("Москва, ул. Ленина, 1", "+375291234567", null, PaymentMethod.CASH_ON_DELIVERY);

    @Mock
    private OrderRepository orders;
    @Mock
    private CartRepository carts;
    @Mock
    private ProductRepository products;
    @Mock
    private UserRepository users;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, carts, products, users, new DirectUnitOfWork(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(orders.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(100L);
            }
            return o;
        });
        lenient().when(users.findById(any())).thenReturn(Optional.empty());
    }

    private Product product(long id, int stock) {
        return new Product(id, "SKU-" + id, null, "Товар " + id, null, new BigDecimal("500.00"), stock, null,
                true, 1L, 1L, Set.of(), NOW, NOW);
    }

    @Test
    void placeOrderReservesStockAndClearsCart() {
        Cart cart = Cart.empty(1L);
        cart.add(10L, 3);
        Product product = product(10L, 5);
        when(carts.findByUserId(1L)).thenReturn(cart);
        when(products.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(product));

        OrderView view = service.placeOrder(1L, COMMAND);

        assertThat(view.number()).isEqualTo("AP-000100");
        assertThat(view.status()).isEqualTo(OrderStatus.NEW);
        assertThat(view.totalAmount()).isEqualByComparingTo("1500.00");
        assertThat(product.getStockQuantity()).isEqualTo(2);
        ArgumentCaptor<Cart> savedCart = ArgumentCaptor.forClass(Cart.class);
        verify(carts).save(savedCart.capture());
        assertThat(savedCart.getValue().isEmpty()).isTrue();
    }

    @Test
    void placeOrderFailsWhenStockIsInsufficient() {
        Cart cart = Cart.empty(1L);
        cart.add(10L, 3);
        when(carts.findByUserId(1L)).thenReturn(cart);
        when(products.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(product(10L, 1)));

        assertThatThrownBy(() -> service.placeOrder(1L, COMMAND)).isInstanceOf(InsufficientStockException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void placeOrderFromEmptyCartFails() {
        when(carts.findByUserId(1L)).thenReturn(Cart.empty(1L));

        assertThatThrownBy(() -> service.placeOrder(1L, COMMAND)).isInstanceOf(ConflictException.class);
    }

    @Test
    void cancellationReturnsItemsToStock() {
        Order order = new Order(100L, 1L, OrderStatus.NEW,
                List.of(new OrderItem(10L, "SKU-10", "Товар", new BigDecimal("500.00"), 2)),
                "Москва", "+375291234567", null, PaymentMethod.CARD_ONLINE, NOW, NOW);
        Product product = product(10L, 1);
        when(orders.findById(100L)).thenReturn(Optional.of(order));
        when(products.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(product));

        OrderView view = service.cancelByCustomer(new AuthenticatedUser(1L, "u@mail.ru", Set.of(Role.CUSTOMER),
                "jti", NOW), 100L);

        assertThat(view.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStockQuantity()).isEqualTo(3);
    }

    @Test
    void customerCannotSeeForeignOrder() {
        Order order = new Order(100L, 2L, OrderStatus.NEW,
                List.of(new OrderItem(10L, "SKU-10", "Товар", new BigDecimal("500.00"), 1)),
                "Москва", "+375291234567", null, PaymentMethod.CARD_ONLINE, NOW, NOW);
        when(orders.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.get(new AuthenticatedUser(1L, "u@mail.ru", Set.of(Role.CUSTOMER),
                "jti", NOW), 100L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void managerCanSeeAnyOrder() {
        Order order = new Order(100L, 2L, OrderStatus.NEW,
                List.of(new OrderItem(10L, "SKU-10", "Товар", new BigDecimal("500.00"), 1)),
                "Москва", "+375291234567", null, PaymentMethod.CARD_ONLINE, NOW, NOW);
        when(orders.findById(100L)).thenReturn(Optional.of(order));

        OrderView view = service.get(new AuthenticatedUser(9L, "m@mail.ru", Set.of(Role.MANAGER), "jti", NOW), 100L);

        assertThat(view.id()).isEqualTo(100L);
    }
}
