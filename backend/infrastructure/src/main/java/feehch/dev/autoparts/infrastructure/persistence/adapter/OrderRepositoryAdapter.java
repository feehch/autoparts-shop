package feehch.dev.autoparts.infrastructure.persistence.adapter;

import feehch.dev.autoparts.application.port.out.OrderRepository;
import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;
import feehch.dev.autoparts.domain.order.Order;
import feehch.dev.autoparts.domain.order.OrderItem;
import feehch.dev.autoparts.domain.order.OrderStatus;
import feehch.dev.autoparts.infrastructure.persistence.entity.OrderEntity;
import feehch.dev.autoparts.infrastructure.persistence.entity.OrderItemEntity;
import feehch.dev.autoparts.infrastructure.persistence.entity.UserEntity;
import feehch.dev.autoparts.infrastructure.persistence.jpa.OrderJpaRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Transactional(readOnly = true)
public class OrderRepositoryAdapter implements OrderRepository {

    private static final Pattern ORDER_NUMBER = Pattern.compile("^(?:AP-?)?0*(\\d{1,18})$", Pattern.CASE_INSENSITIVE);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OrderJpaRepository jpa;

    public OrderRepositoryAdapter(OrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderEntity entity;
        if (order.getId() == null) {
            entity = new OrderEntity();
            entity.setUserId(order.getUserId());
            entity.setTotalAmount(order.getTotalAmount());
            entity.setDeliveryAddress(order.getDeliveryAddress());
            entity.setContactPhone(order.getContactPhone());
            entity.setComment(order.getComment());
            entity.setPaymentMethod(order.getPaymentMethod());
            entity.setCreatedAt(order.getCreatedAt());
            for (OrderItem item : order.getItems()) {
                OrderItemEntity itemEntity = new OrderItemEntity();
                itemEntity.setProductId(item.productId());
                itemEntity.setSku(item.sku());
                itemEntity.setName(item.name());
                itemEntity.setPrice(item.price());
                itemEntity.setQuantity(item.quantity());
                entity.addItem(itemEntity);
            }
        } else {
            // позиции заказа неизменяемы, обновляется только статус
            entity = jpa.findById(order.getId()).orElseThrow();
        }
        entity.setStatus(order.getStatus());
        entity.setUpdatedAt(order.getUpdatedAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpa.findById(id).map(OrderRepositoryAdapter::toDomain);
    }

    @Override
    public Page<Order> findByUserId(Long userId, PageQuery pageQuery) {
        var result = jpa.findByUserId(userId, PageRequest.of(pageQuery.page(), pageQuery.size(), NEWEST_FIRST));
        return Page.of(result.getContent().stream().map(OrderRepositoryAdapter::toDomain).toList(), pageQuery,
                result.getTotalElements());
    }

    @Override
    public Page<Order> search(OrderStatus status, String query, PageQuery pageQuery) {
        Specification<OrderEntity> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (query != null) {
                Subquery<Long> users = cq.subquery(Long.class);
                Root<UserEntity> user = users.from(UserEntity.class);
                users.select(user.get("id"))
                        .where(cb.like(cb.lower(user.get("email")), "%" + query.toLowerCase(Locale.ROOT) + "%"));
                Predicate byEmail = root.get("userId").in(users);
                Matcher number = ORDER_NUMBER.matcher(query);
                predicates.add(number.matches()
                        ? cb.or(byEmail, cb.equal(root.get("id"), Long.parseLong(number.group(1))))
                        : byEmail);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var result = jpa.findAll(spec, PageRequest.of(pageQuery.page(), pageQuery.size(), NEWEST_FIRST));
        return Page.of(result.getContent().stream().map(OrderRepositoryAdapter::toDomain).toList(), pageQuery,
                result.getTotalElements());
    }

    @Override
    public Map<OrderStatus, Long> countByStatus() {
        Map<OrderStatus, Long> result = new EnumMap<>(OrderStatus.class);
        for (Object[] row : jpa.countGroupedByStatus()) {
            result.put((OrderStatus) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public BigDecimal sumTotalByStatus(OrderStatus status) {
        return jpa.sumTotalByStatus(status);
    }

    private static Order toDomain(OrderEntity e) {
        List<OrderItem> items = e.getItems().stream()
                .map(i -> new OrderItem(i.getProductId(), i.getSku(), i.getName(), i.getPrice(), i.getQuantity()))
                .toList();
        return new Order(e.getId(), e.getUserId(), e.getStatus(), items, e.getDeliveryAddress(),
                e.getContactPhone(), e.getComment(), e.getPaymentMethod(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
