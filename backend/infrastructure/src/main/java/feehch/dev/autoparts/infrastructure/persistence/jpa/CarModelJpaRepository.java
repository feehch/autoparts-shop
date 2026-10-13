package feehch.dev.autoparts.infrastructure.persistence.jpa;

import feehch.dev.autoparts.infrastructure.persistence.entity.CarModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarModelJpaRepository extends JpaRepository<CarModelEntity, Long> {

    List<CarModelEntity> findByMakeId(Long makeId);

    boolean existsByMakeId(Long makeId);
}
