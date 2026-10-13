package feehch.dev.autoparts.application.port.out;

import feehch.dev.autoparts.domain.catalog.CarModel;

import java.util.List;
import java.util.Optional;

/**
 * Порт хранилища моделей автомобилей.
 */
public interface CarModelRepository {

    List<CarModel> findAll();

    List<CarModel> findByMakeId(Long makeId);

    Optional<CarModel> findById(Long id);

    CarModel save(CarModel model);

    void deleteById(Long id);

    boolean existsByMakeId(Long makeId);
}
