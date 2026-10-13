package feehch.dev.autoparts.application.order;

import feehch.dev.autoparts.application.port.out.CartRepository;
import feehch.dev.autoparts.application.port.out.OrderRepository;
import feehch.dev.autoparts.application.port.out.ProductRepository;
import feehch.dev.autoparts.application.port.out.UnitOfWork;
import feehch.dev.autoparts.application.port.out.UserRepository;
import feehch.dev.autoparts.application.security.AccessDeniedException;
import feehch.dev.autoparts.application.security.AuthenticatedUser;
import feehch.dev.autoparts.domain.cart.Cart;
import feehch.dev.autoparts.domain.cart.CartItem;
import feehch.dev.autoparts.domain.catalog.Product;
import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.common.NotFoundException;
import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;
import feehch.dev.autoparts.domain.order.Order;
import feehch.dev.autoparts.domain.order.OrderItem;
import feehch.dev.autoparts.domain.order.OrderStatus;
import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.domain.user.User;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сценарии оформления и обработки заказов.
 */
public class OrderService {

    private final OrderRepository orders;
    private final CartRepository carts;
    private final ProductRepository products;
    private final UserRepository users;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public OrderService(OrderRepository orders, CartRepository carts, ProductRepository products,
                        UserRepository users, UnitOfWork unitOfWork, Clock clock) {
        this.orders = orders;
        this.carts = carts;
        this.products = products;
        this.users = users;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    /**
     * Оформляет заказ из корзины: фиксирует цены, списывает остатки и очищает корзину — атомарно.
     */
    public OrderView placeOrder(Long userId, PlaceOrderCommand command) {
        Order saved = unitOfWork.inTransaction(() -> {
            Cart cart = carts.findByUserId(userId);
            if (cart.isEmpty()) {
                throw new ConflictException("Корзина пуста");
            }
            Instant now = clock.instant();
            List<Long> productIds = cart.getItems().stream().map(CartItem::productId).sorted().toList();
            Map<Long, Product> productById = products.findAllByIdForUpdate(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));

            List<OrderItem> items = new ArrayList<>();
            for (CartItem cartItem : cart.getItems()) {
                Product product = productById.get(cartItem.productId());
                if (product == null) {
                    throw NotFoundException.of("Товар", cartItem.productId());
                }
                product.reserve(cartItem.quantity(), now);
                products.save(product);
                items.add(new OrderItem(product.getId(), product.getSku(), product.getName(), product.getPrice(),
                        cartItem.quantity()));
            }
            Order order = Order.place(userId, items, command.deliveryAddress(), command.contactPhone(),
                    command.comment(), command.paymentMethod(), now);
            Order result = orders.save(order);
            cart.clear();
            carts.save(cart);
            return result;
        });
        return toView(saved);
    }

    public Page<OrderView> myOrders(Long userId, PageQuery pageQuery) {
        User customer = users.findById(userId).orElse(null);
        return orders.findByUserId(userId, pageQuery).map(o -> OrderView.from(o, customer));
    }

    public OrderView get(AuthenticatedUser actor, Long orderId) {
        Order order = load(orderId);
        ensureCanView(actor, order);
        return toView(order);
    }

    public OrderView cancelByCustomer(AuthenticatedUser actor, Long orderId) {
        Order order = unitOfWork.inTransaction(() -> {
            Order o = load(orderId);
            if (!o.belongsTo(actor.id())) {
                throw new AccessDeniedException("Можно отменить только свой заказ");
            }
            o.cancelByCustomer(clock.instant());
            returnToStock(o);
            return orders.save(o);
        });
        return toView(order);
    }

    public Page<OrderView> search(OrderStatus status, String query, PageQuery pageQuery) {
        Page<Order> page = orders.search(status, query == null || query.isBlank() ? null : query.trim(), pageQuery);
        Map<Long, User> customers = users.findAllById(page.content().stream().map(Order::getUserId)
                        .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return page.map(o -> OrderView.from(o, customers.get(o.getUserId())));
    }

    /**
     * Смена статуса менеджером. При отмене товары возвращаются на склад.
     */
    public OrderView changeStatus(Long orderId, OrderStatus status) {
        Order order = unitOfWork.inTransaction(() -> {
            Order o = load(orderId);
            o.changeStatus(status, clock.instant());
            if (status == OrderStatus.CANCELLED) {
                returnToStock(o);
            }
            return orders.save(o);
        });
        return toView(order);
    }

    private void returnToStock(Order order) {
        Instant now = clock.instant();
        List<Long> ids = order.getItems().stream().map(OrderItem::productId).sorted().toList();
        Map<Long, Product> productById = products.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (OrderItem item : order.getItems()) {
            Product product = productById.get(item.productId());
            if (product != null) {
                product.release(item.quantity(), now);
                products.save(product);
            }
        }
    }

    private void ensureCanView(AuthenticatedUser actor, Order order) {
        if (!order.belongsTo(actor.id()) && !actor.hasRole(Role.MANAGER)) {
            throw new AccessDeniedException("Нет доступа к заказу");
        }
    }

    private Order load(Long id) {
        return orders.findById(id).orElseThrow(() -> NotFoundException.of("Заказ", id));
    }

    private OrderView toView(Order order) {
        return OrderView.from(order, users.findById(order.getUserId()).orElse(null));
    }
}
