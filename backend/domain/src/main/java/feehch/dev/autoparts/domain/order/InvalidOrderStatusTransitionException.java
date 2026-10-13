package feehch.dev.autoparts.domain.order;

import feehch.dev.autoparts.domain.common.ConflictException;

/**
 * Попытка перевести заказ в статус, недопустимый из текущего.
 */
public class InvalidOrderStatusTransitionException extends ConflictException {

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Нельзя перевести заказ из статуса " + from + " в статус " + to);
    }
}
