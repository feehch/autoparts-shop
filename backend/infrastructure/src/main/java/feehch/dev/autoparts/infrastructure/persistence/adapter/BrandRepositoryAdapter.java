package feehch.dev.autoparts.infrastructure.persistence.adapter;

import feehch.dev.autoparts.application.port.out.BrandRepository;
import feehch.dev.autoparts.domain.catalog.Brand;
import feehch.dev.autoparts.infrastructure.cache.CacheNames;
import feehch.dev.autoparts.infrastructure.persistence.entity.BrandEntity;
import feehch.dev.autoparts.infrastructure.persistence.jpa.BrandJpaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class BrandRepositoryAdapter implements BrandRepository {

    private final BrandJpaRepository jpa;

    public BrandRepositoryAdapter(BrandJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.BRANDS, key = "'all'")
    public List<Brand> findAll() {
        return new ArrayList<>(jpa.findAll().stream().map(BrandRepositoryAdapter::toDomain).toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.BRANDS, key = "#id")
    public Optional<Brand> findById(Long id) {
        return jpa.findById(id).map(BrandRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.BRANDS, allEntries = true)
    public Brand save(Brand brand) {
        BrandEntity entity = brand.getId() == null ? new BrandEntity()
                : jpa.findById(brand.getId()).orElseGet(BrandEntity::new);
        entity.setName(brand.getName());
        entity.setCountry(brand.getCountry());
        entity.setDescription(brand.getDescription());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.BRANDS, allEntries = true)
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsByName(String name, Long excludeId) {
        return excludeId == null ? jpa.existsByNameIgnoreCase(name) : jpa.existsByNameIgnoreCaseAndIdNot(name, excludeId);
    }

    private static Brand toDomain(BrandEntity e) {
        return new Brand(e.getId(), e.getName(), e.getCountry(), e.getDescription());
    }
}
