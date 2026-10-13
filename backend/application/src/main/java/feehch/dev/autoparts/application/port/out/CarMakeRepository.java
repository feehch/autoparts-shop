package feehch.dev.autoparts.application.port.out;

import feehch.dev.autoparts.domain.catalog.CarMake;

import java.util.List;
import java.util.Optional;

/**
 * Порт хранилища марок автомобилей.
 */
public interface CarMakeRepository {

    List<CarMake> findAll();

    Optional<CarMake> findById(Long id);

    CarMake save(CarMake make);

    void deleteById(Long id);

    boolean existsByName(String name, Long excludeId);
}
