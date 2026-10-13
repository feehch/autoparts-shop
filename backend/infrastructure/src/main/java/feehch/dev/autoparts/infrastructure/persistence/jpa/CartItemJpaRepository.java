package feehch.dev.autoparts.infrastructure.persistence.jpa;

import feehch.dev.autoparts.infrastructure.persistence.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemJpaRepository extends JpaRepository<CartItemEntity, CartItemEntity.Key> {

    @Query("select c from CartItemEntity c where c.id.userId = :userId order by c.addedAt, c.id.productId")
    List<CartItemEntity> findByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CartItemEntity c where c.id.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
