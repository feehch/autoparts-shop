package feehch.dev.autoparts.application.order;

import feehch.dev.autoparts.domain.order.Order;
import feehch.dev.autoparts.domain.order.OrderStatus;
import feehch.dev.autoparts.domain.order.PaymentMethod;
import feehch.dev.autoparts.domain.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderView(Long id, String number, Long userId, String customerEmail, String customerName,
                        OrderStatus status, List<OrderItemView> items, BigDecimal totalAmount,
                        String deliveryAddress, String contactPhone, String comment, PaymentMethod paymentMethod,
                        Instant createdAt, Instant updatedAt) {

    public static OrderView from(Order order, User customer) {
        return new OrderView(order.getId(), order.number(), order.getUserId(),
                customer == null ? null : customer.getEmail(),
                customer == null ? null : customer.fullName(),
                order.getStatus(), order.getItems().stream().map(OrderItemView::from).toList(),
                order.getTotalAmount(), order.getDeliveryAddress(), order.getContactPhone(), order.getComment(),
                order.getPaymentMethod(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
