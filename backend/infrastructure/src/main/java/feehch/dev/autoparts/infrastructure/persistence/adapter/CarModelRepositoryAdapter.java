package feehch.dev.autoparts.infrastructure.persistence.adapter;

import feehch.dev.autoparts.application.port.out.CarModelRepository;
import feehch.dev.autoparts.domain.catalog.CarModel;
import feehch.dev.autoparts.infrastructure.cache.CacheNames;
import feehch.dev.autoparts.infrastructure.persistence.entity.CarModelEntity;
import feehch.dev.autoparts.infrastructure.persistence.jpa.CarModelJpaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class CarModelRepositoryAdapter implements CarModelRepository {

    private final CarModelJpaRepository jpa;

    public CarModelRepositoryAdapter(CarModelJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CAR_MODELS, key = "'all'")
    public List<CarModel> findAll() {
        return new ArrayList<>(jpa.findAll().stream().map(CarModelRepositoryAdapter::toDomain).toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CAR_MODELS, key = "'make:' + #makeId")
    public List<CarModel> findByMakeId(Long makeId) {
        return new ArrayList<>(jpa.findByMakeId(makeId).stream().map(CarModelRepositoryAdapter::toDomain).toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CAR_MODELS, key = "#id")
    public Optional<CarModel> findById(Long id) {
        return jpa.findById(id).map(CarModelRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CAR_MODELS, allEntries = true)
    public CarModel save(CarModel model) {
        CarModelEntity entity = model.getId() == null ? new CarModelEntity()
                : jpa.findById(model.getId()).orElseGet(CarModelEntity::new);
        entity.setMakeId(model.getMakeId());
        entity.setName(model.getName());
        entity.setYearFrom(model.getYearFrom());
        entity.setYearTo(model.getYearTo());
        return toDomain(jpa.save(entity));
    }

    /**
     * Удаление модели также удаляет её из списков совместимости товаров (ON DELETE CASCADE),
     * поэтому кэш карточек товаров сбрасывается.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CAR_MODELS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true)
    })
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsByMakeId(Long makeId) {
        return jpa.existsByMakeId(makeId);
    }

    private static CarModel toDomain(CarModelEntity e) {
        return new CarModel(e.getId(), e.getMakeId(), e.getName(), e.getYearFrom(), e.getYearTo());
    }
}
