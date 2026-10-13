package feehch.dev.autoparts.application.stats;

import feehch.dev.autoparts.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardView(long productsCount, long activeProductsCount, long lowStockCount, long ordersCount,
                            Map<OrderStatus, Long> ordersByStatus, BigDecimal revenue, long customersCount) {
}
