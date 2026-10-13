package feehch.dev.autoparts.application.order;

import feehch.dev.autoparts.domain.order.OrderItem;

import java.math.BigDecimal;

public record OrderItemView(Long productId, String sku, String name, BigDecimal price, int quantity,
                            BigDecimal lineTotal) {

    public static OrderItemView from(OrderItem item) {
        return new OrderItemView(item.productId(), item.sku(), item.name(), item.price(), item.quantity(),
                item.lineTotal());
    }
}
