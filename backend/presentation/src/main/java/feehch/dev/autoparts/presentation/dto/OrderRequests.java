package feehch.dev.autoparts.presentation.dto;

import feehch.dev.autoparts.application.order.PlaceOrderCommand;
import feehch.dev.autoparts.domain.order.OrderStatus;
import feehch.dev.autoparts.domain.order.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Запросы корзины и заказов.
 */
public final class OrderRequests {

    private OrderRequests() {
    }

    public record AddCartItem(@NotNull Long productId, @NotNull @Min(1) @Max(999) Integer quantity) {
    }

    public record UpdateCartItem(@NotNull @Min(1) @Max(999) Integer quantity) {
    }

    public record PlaceOrder(
            @NotBlank @Size(max = 500) String deliveryAddress,
            @NotBlank @Pattern(regexp = AuthRequests.PHONE_REGEX, message = AuthRequests.PHONE_MESSAGE) String contactPhone,
            @Size(max = 1000) String comment,
            @NotNull PaymentMethod paymentMethod) {

        public PlaceOrderCommand toCommand() {
            return new PlaceOrderCommand(deliveryAddress, contactPhone, comment, paymentMethod);
        }
    }

    public record ChangeStatus(@NotNull OrderStatus status) {
    }
}
