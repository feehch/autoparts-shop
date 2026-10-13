package feehch.dev.autoparts.application.order;

import feehch.dev.autoparts.domain.order.PaymentMethod;

public record PlaceOrderCommand(String deliveryAddress, String contactPhone, String comment,
                                PaymentMethod paymentMethod) {
}
