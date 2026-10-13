package feehch.dev.autoparts.application.stats;

import feehch.dev.autoparts.application.port.out.OrderRepository;
import feehch.dev.autoparts.application.port.out.ProductRepository;
import feehch.dev.autoparts.application.port.out.UserRepository;
import feehch.dev.autoparts.domain.catalog.Product;
import feehch.dev.autoparts.domain.order.OrderStatus;
import feehch.dev.autoparts.domain.user.Role;

import java.util.EnumMap;
import java.util.Map;

/**
 * Сводная статистика магазина для панели управления.
 */
public class StatsService {

    private final ProductRepository products;
    private final OrderRepository orders;
    private final UserRepository users;

    public StatsService(ProductRepository products, OrderRepository orders, UserRepository users) {
        this.products = products;
        this.orders = orders;
        this.users = users;
    }

    public DashboardView dashboard() {
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status, 0L);
        }
        byStatus.putAll(orders.countByStatus());
        long ordersCount = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new DashboardView(
                products.count(),
                products.countActive(),
                products.countActiveWithStockBelow(Product.LOW_STOCK_THRESHOLD),
                ordersCount,
                byStatus,
                orders.sumTotalByStatus(OrderStatus.DELIVERED),
                users.countByRole(Role.CUSTOMER));
    }
}
