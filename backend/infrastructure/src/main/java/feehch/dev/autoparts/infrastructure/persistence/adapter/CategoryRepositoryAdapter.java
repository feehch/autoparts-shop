package feehch.dev.autoparts.infrastructure.persistence.adapter;

import feehch.dev.autoparts.application.port.out.CategoryRepository;
import feehch.dev.autoparts.domain.catalog.Category;
import feehch.dev.autoparts.infrastructure.cache.CacheNames;
import feehch.dev.autoparts.infrastructure.persistence.entity.CategoryEntity;
import feehch.dev.autoparts.infrastructure.persistence.jpa.CategoryJpaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository jpa;

    public CategoryRepositoryAdapter(CategoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CATEGORIES, key = "'all'")
    public List<Category> findAll() {
        return new ArrayList<>(jpa.findAll().stream().map(CategoryRepositoryAdapter::toDomain).toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CATEGORIES, key = "#id")
    public Optional<Category> findById(Long id) {
        return jpa.findById(id).map(CategoryRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CATEGORIES, allEntries = true)
    public Category save(Category category) {
        CategoryEntity entity = category.getId() == null ? new CategoryEntity()
                : jpa.findById(category.getId()).orElseGet(CategoryEntity::new);
        entity.setName(category.getName());
        entity.setSlug(category.getSlug());
        entity.setDescription(category.getDescription());
        entity.setParentId(category.getParentId());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CATEGORIES, allEntries = true)
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsBySlug(String slug, Long excludeId) {
        return excludeId == null ? jpa.existsBySlug(slug) : jpa.existsBySlugAndIdNot(slug, excludeId);
    }

    @Override
    public boolean hasChildren(Long id) {
        return jpa.existsByParentId(id);
    }

    private static Category toDomain(CategoryEntity e) {
        return new Category(e.getId(), e.getName(), e.getSlug(), e.getDescription(), e.getParentId());
    }
}
