package feehch.dev.autoparts.infrastructure.persistence.adapter;

import feehch.dev.autoparts.application.port.out.ProductRepository;
import feehch.dev.autoparts.domain.catalog.Product;
import feehch.dev.autoparts.domain.catalog.ProductSearchCriteria;
import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;
import feehch.dev.autoparts.infrastructure.cache.CacheNames;
import feehch.dev.autoparts.infrastructure.persistence.entity.CarModelEntity;
import feehch.dev.autoparts.infrastructure.persistence.entity.ProductEntity;
import feehch.dev.autoparts.infrastructure.persistence.jpa.ProductJpaRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;

    public ProductRepositoryAdapter(ProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PRODUCTS, key = "#id")
    public Optional<Product> findById(Long id) {
        return jpa.findById(id).map(ProductRepositoryAdapter::toDomain);
    }

    @Override
    public List<Product> findAllById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpa.findAllById(ids).stream().map(ProductRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional
    public List<Product> findAllByIdForUpdate(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpa.findAllByIdForUpdate(ids).stream().map(ProductRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCTS, key = "#product.id", condition = "#product.id != null")
    public Product save(Product product) {
        ProductEntity entity = product.getId() == null ? new ProductEntity()
                : jpa.findById(product.getId()).orElseGet(ProductEntity::new);
        entity.setSku(product.getSku());
        entity.setOemNumber(product.getOemNumber());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setPrice(product.getPrice());
        entity.setStockQuantity(product.getStockQuantity());
        entity.setImageUrl(product.getImageUrl());
        entity.setActive(product.isActive());
        entity.setBrandId(product.getBrandId());
        entity.setCategoryId(product.getCategoryId());
        entity.getCompatibleModelIds().clear();
        entity.getCompatibleModelIds().addAll(product.getCompatibleModelIds());
        entity.setCreatedAt(product.getCreatedAt());
        entity.setUpdatedAt(product.getUpdatedAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Page<Product> search(ProductSearchCriteria criteria, PageQuery pageQuery) {
        Sort sort = switch (criteria.sort()) {
            case NAME_ASC -> Sort.by("name").ascending();
            case NAME_DESC -> Sort.by("name").descending();
            case PRICE_ASC -> Sort.by("price").ascending();
            case PRICE_DESC -> Sort.by("price").descending();
            case NEWEST -> Sort.by("createdAt").descending();
        };
        var result = jpa.findAll(toSpecification(criteria),
                PageRequest.of(pageQuery.page(), pageQuery.size(), sort.and(Sort.by("id"))));
        return Page.of(result.getContent().stream().map(ProductRepositoryAdapter::toDomain).toList(), pageQuery,
                result.getTotalElements());
    }

    @Override
    public boolean existsBySku(String sku, Long excludeId) {
        return excludeId == null ? jpa.existsBySkuIgnoreCase(sku) : jpa.existsBySkuIgnoreCaseAndIdNot(sku, excludeId);
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return jpa.existsByCategoryId(categoryId);
    }

    @Override
    public boolean existsByBrandId(Long brandId) {
        return jpa.existsByBrandId(brandId);
    }

    @Override
    public long count() {
        return jpa.count();
    }

    @Override
    public long countActive() {
        return jpa.countByActiveTrue();
    }

    @Override
    public long countActiveWithStockBelow(int threshold) {
        return jpa.countByActiveTrueAndStockQuantityLessThan(threshold);
    }

    /**
     * Строит динамический запрос (JPA Criteria API) по критериям поиска.
     */
    private static Specification<ProductEntity> toSpecification(ProductSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!c.includeInactive()) {
                predicates.add(cb.isTrue(root.get("active")));
            }
            if (c.query() != null) {
                String like = "%" + c.query().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("sku")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("oemNumber"), "")), like)));
            }
            if (c.categoryIds() != null) {
                predicates.add(root.get("categoryId").in(c.categoryIds()));
            }
            if (c.brandId() != null) {
                predicates.add(cb.equal(root.get("brandId"), c.brandId()));
            }
            if (c.modelId() != null) {
                predicates.add(cb.isMember(c.modelId(), root.<Collection<Long>>get("compatibleModelIds")));
            } else if (c.makeId() != null) {
                // товары, совместимые хотя бы с одной моделью выбранной марки
                Subquery<Long> sub = query.subquery(Long.class);
                Root<ProductEntity> p = sub.from(ProductEntity.class);
                Join<ProductEntity, Long> modelId = p.join("compatibleModelIds");
                Root<CarModelEntity> model = sub.from(CarModelEntity.class);
                sub.select(p.get("id")).where(
                        cb.equal(model.get("id"), modelId),
                        cb.equal(model.get("makeId"), c.makeId()));
                predicates.add(root.get("id").in(sub));
            }
            if (c.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), c.minPrice()));
            }
            if (c.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), c.maxPrice()));
            }
            if (c.inStockOnly()) {
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static Product toDomain(ProductEntity e) {
        return new Product(e.getId(), e.getSku(), e.getOemNumber(), e.getName(), e.getDescription(), e.getPrice(),
                e.getStockQuantity(), e.getImageUrl(), e.isActive(), e.getBrandId(), e.getCategoryId(),
                e.getCompatibleModelIds(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
