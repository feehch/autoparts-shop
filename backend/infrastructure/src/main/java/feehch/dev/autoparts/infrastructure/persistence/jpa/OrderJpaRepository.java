package feehch.dev.autoparts.infrastructure.persistence.jpa;

import feehch.dev.autoparts.domain.order.OrderStatus;
import feehch.dev.autoparts.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {

    Page<OrderEntity> findByUserId(Long userId, Pageable pageable);

    @Query("select o.status, count(o) from OrderEntity o group by o.status")
    List<Object[]> countGroupedByStatus();

    @Query("select coalesce(sum(o.totalAmount), 0) from OrderEntity o where o.status = :status")
    BigDecimal sumTotalByStatus(@Param("status") OrderStatus status);
}
