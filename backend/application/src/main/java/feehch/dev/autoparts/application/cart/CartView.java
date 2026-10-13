package feehch.dev.autoparts.application.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartView(List<CartItemView> items, int totalQuantity, BigDecimal totalAmount) {
}
