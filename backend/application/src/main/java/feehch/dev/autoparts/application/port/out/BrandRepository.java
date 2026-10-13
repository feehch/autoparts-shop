package feehch.dev.autoparts.application.port.out;

import feehch.dev.autoparts.domain.catalog.Brand;

import java.util.List;
import java.util.Optional;

/**
 * Порт хранилища брендов.
 */
public interface BrandRepository {

    List<Brand> findAll();

    Optional<Brand> findById(Long id);

    Brand save(Brand brand);

    void deleteById(Long id);

    boolean existsByName(String name, Long excludeId);
}
