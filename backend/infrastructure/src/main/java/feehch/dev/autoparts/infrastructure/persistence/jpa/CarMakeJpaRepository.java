package feehch.dev.autoparts.infrastructure.persistence.jpa;

import feehch.dev.autoparts.infrastructure.persistence.entity.CarMakeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarMakeJpaRepository extends JpaRepository<CarMakeEntity, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
