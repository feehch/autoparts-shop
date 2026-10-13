package feehch.dev.autoparts.infrastructure.persistence.adapter;

import feehch.dev.autoparts.application.port.out.CarMakeRepository;
import feehch.dev.autoparts.domain.catalog.CarMake;
import feehch.dev.autoparts.infrastructure.cache.CacheNames;
import feehch.dev.autoparts.infrastructure.persistence.entity.CarMakeEntity;
import feehch.dev.autoparts.infrastructure.persistence.jpa.CarMakeJpaRepository;
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
public class CarMakeRepositoryAdapter implements CarMakeRepository {

    private final CarMakeJpaRepository jpa;

    public CarMakeRepositoryAdapter(CarMakeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CAR_MAKES, key = "'all'")
    public List<CarMake> findAll() {
        return new ArrayList<>(jpa.findAll().stream().map(CarMakeRepositoryAdapter::toDomain).toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CAR_MAKES, key = "#id")
    public Optional<CarMake> findById(Long id) {
        return jpa.findById(id).map(CarMakeRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CAR_MAKES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.CAR_MODELS, allEntries = true)
    })
    public CarMake save(CarMake make) {
        CarMakeEntity entity = make.getId() == null ? new CarMakeEntity()
                : jpa.findById(make.getId()).orElseGet(CarMakeEntity::new);
        entity.setName(make.getName());
        entity.setCountry(make.getCountry());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CAR_MAKES, allEntries = true)
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsByName(String name, Long excludeId) {
        return excludeId == null ? jpa.existsByNameIgnoreCase(name) : jpa.existsByNameIgnoreCaseAndIdNot(name, excludeId);
    }

    private static CarMake toDomain(CarMakeEntity e) {
        return new CarMake(e.getId(), e.getName(), e.getCountry());
    }
}
