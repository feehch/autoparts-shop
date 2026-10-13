package feehch.dev.autoparts.application.port.out;

import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;
import feehch.dev.autoparts.domain.order.Order;
import feehch.dev.autoparts.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Порт хранилища заказов.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    Page<Order> findByUserId(Long userId, PageQuery pageQuery);

    /**
     * Поиск заказов по статусу и строке (номер заказа или email покупателя).
     */
    Page<Order> search(OrderStatus status, String query, PageQuery pageQuery);

    Map<OrderStatus, Long> countByStatus();

    BigDecimal sumTotalByStatus(OrderStatus status);
}
