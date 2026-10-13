package feehch.dev.autoparts.domain.order;

import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.common.DomainException;
import feehch.dev.autoparts.domain.common.Guard;
import feehch.dev.autoparts.domain.common.PhoneNumber;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Заказ покупателя.
 */
public class Order {

    private Long id;
    private final Long userId;
    private OrderStatus status;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private final String deliveryAddress;
    private final String contactPhone;
    private final String comment;
    private final PaymentMethod paymentMethod;
    private final Instant createdAt;
    private Instant updatedAt;

    public Order(Long id, Long userId, OrderStatus status, List<OrderItem> items, String deliveryAddress,
                 String contactPhone, String comment, PaymentMethod paymentMethod, Instant createdAt,
                 Instant updatedAt) {
        if (items == null || items.isEmpty()) {
            throw new DomainException("Заказ должен содержать хотя бы одну позицию");
        }
        this.id = id;
        this.userId = Guard.notNull(userId, "пользователь");
        this.status = Guard.notNull(status, "статус");
        this.items = List.copyOf(items);
        this.totalAmount = items.stream().map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        this.deliveryAddress = Guard.notBlank(deliveryAddress, "адрес доставки");
        this.contactPhone = PhoneNumber.normalize(Guard.notBlank(contactPhone, "контактный телефон"));
        this.comment = Guard.trimToNull(comment);
        this.paymentMethod = Guard.notNull(paymentMethod, "способ оплаты");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Оформляет новый заказ.
     */
    public static Order place(Long userId, List<OrderItem> items, String deliveryAddress, String contactPhone,
                              String comment, PaymentMethod paymentMethod, Instant now) {
        return new Order(null, userId, OrderStatus.NEW, items, deliveryAddress, contactPhone, comment,
                paymentMethod, now, now);
    }

    /**
     * Переводит заказ в новый статус, проверяя допустимость перехода.
     */
    public void changeStatus(OrderStatus target, Instant now) {
        Guard.notNull(target, "статус");
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderStatusTransitionException(status, target);
        }
        this.status = target;
        this.updatedAt = now;
    }

    /**
     * Отмена заказа самим покупателем — возможна только до подтверждения менеджером.
     */
    public void cancelByCustomer(Instant now) {
        if (status != OrderStatus.NEW) {
            throw new ConflictException("Покупатель может отменить только новый (неподтверждённый) заказ");
        }
        changeStatus(OrderStatus.CANCELLED, now);
    }

    public boolean belongsTo(Long userId) {
        return this.userId.equals(userId);
    }

    /**
     * Человекочитаемый номер заказа.
     */
    public String number() {
        return formatNumber(id);
    }

    public static String formatNumber(Long id) {
        return id == null ? null : String.format("AP-%06d", id);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getComment() {
        return comment;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
