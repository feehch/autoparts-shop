package feehch.dev.autoparts.application.catalog;

import feehch.dev.autoparts.application.port.out.BrandRepository;
import feehch.dev.autoparts.application.port.out.CarMakeRepository;
import feehch.dev.autoparts.application.port.out.CarModelRepository;
import feehch.dev.autoparts.application.port.out.CategoryRepository;
import feehch.dev.autoparts.application.port.out.ProductRepository;
import feehch.dev.autoparts.application.port.out.UnitOfWork;
import feehch.dev.autoparts.domain.catalog.Brand;
import feehch.dev.autoparts.domain.catalog.CarMake;
import feehch.dev.autoparts.domain.catalog.CarModel;
import feehch.dev.autoparts.domain.catalog.Category;
import feehch.dev.autoparts.domain.catalog.Product;
import feehch.dev.autoparts.domain.catalog.ProductSearchCriteria;
import feehch.dev.autoparts.domain.catalog.ProductSort;
import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.common.DomainException;
import feehch.dev.autoparts.domain.common.NotFoundException;
import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Каталог товаров: поиск и просмотр (для всех), управление товарами (для менеджеров).
 */
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final CarMakeRepository makes;
    private final CarModelRepository models;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public ProductService(ProductRepository products, CategoryRepository categories, BrandRepository brands,
                          CarMakeRepository makes, CarModelRepository models, UnitOfWork unitOfWork, Clock clock) {
        this.products = products;
        this.categories = categories;
        this.brands = brands;
        this.makes = makes;
        this.models = models;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    /**
     * Поиск товаров. Неактивные товары доступны только сотрудникам магазина ({@code canSeeInactive}).
     */
    public Page<ProductSummaryView> search(ProductSearchQuery query, PageQuery pageQuery, boolean canSeeInactive) {
        if (query.minPrice() != null && query.maxPrice() != null && query.minPrice().compareTo(query.maxPrice()) > 0) {
            throw new DomainException("Минимальная цена не может быть больше максимальной");
        }
        List<Category> allCategories = categories.findAll();
        Set<Long> categoryIds = query.categoryId() == null ? null
                : Category.withDescendants(query.categoryId(), allCategories);
        ProductSearchCriteria criteria = new ProductSearchCriteria(
                query.q() == null || query.q().isBlank() ? null : query.q().trim(),
                categoryIds, query.brandId(), query.makeId(), query.modelId(),
                query.minPrice(), query.maxPrice(), query.inStock(),
                canSeeInactive && query.includeInactive(), ProductSort.fromCode(query.sort()));

        Map<Long, String> brandNames = brands.findAll().stream()
                .collect(Collectors.toMap(Brand::getId, Brand::getName));
        Map<Long, String> categoryNames = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        return products.search(criteria, pageQuery)
                .map(p -> toSummary(p, brandNames.get(p.getBrandId()), categoryNames.get(p.getCategoryId())));
    }

    public ProductView get(Long id, boolean canSeeInactive) {
        Product product = load(id);
        if (!product.isActive() && !canSeeInactive) {
            throw NotFoundException.of("Товар", id);
        }
        return toView(product);
    }

    public ProductView create(ProductCommand command) {
        return unitOfWork.inTransaction(() -> {
            validateReferences(command);
            ensureSkuUnique(command.sku(), null);
            Product product = Product.create(command.sku(), command.oemNumber(), command.name(),
                    command.description(), command.price(), command.stockQuantity(), command.imageUrl(),
                    command.active(), command.brandId(), command.categoryId(), command.compatibleModelIds(),
                    clock.instant());
            return toView(products.save(product));
        });
    }

    public ProductView update(Long id, ProductCommand command) {
        return unitOfWork.inTransaction(() -> {
            Product product = load(id);
            validateReferences(command);
            ensureSkuUnique(command.sku(), id);
            product.update(command.sku(), command.oemNumber(), command.name(), command.description(),
                    command.price(), command.stockQuantity(), command.imageUrl(), command.active(),
                    command.brandId(), command.categoryId(), command.compatibleModelIds(), clock.instant());
            return toView(products.save(product));
        });
    }

    public ProductView changeStock(Long id, int stockQuantity) {
        return unitOfWork.inTransaction(() -> {
            Product product = load(id);
            product.changeStock(stockQuantity, clock.instant());
            return toView(products.save(product));
        });
    }

    /**
     * Мягкое удаление: товар скрывается из каталога, но остаётся в истории заказов.
     */
    public void deactivate(Long id) {
        unitOfWork.inTransaction(() -> {
            Product product = load(id);
            product.deactivate(clock.instant());
            products.save(product);
        });
    }

    private void validateReferences(ProductCommand command) {
        if (command.brandId() == null || brands.findById(command.brandId()).isEmpty()) {
            throw NotFoundException.of("Бренд", command.brandId());
        }
        if (command.categoryId() == null || categories.findById(command.categoryId()).isEmpty()) {
            throw NotFoundException.of("Категория", command.categoryId());
        }
        if (command.compatibleModelIds() != null && !command.compatibleModelIds().isEmpty()) {
            Set<Long> known = models.findAll().stream().map(CarModel::getId).collect(Collectors.toSet());
            command.compatibleModelIds().stream().filter(modelId -> !known.contains(modelId)).findFirst()
                    .ifPresent(missing -> {
                        throw NotFoundException.of("Модель автомобиля", missing);
                    });
        }
    }

    private void ensureSkuUnique(String sku, Long excludeId) {
        if (sku != null && products.existsBySku(sku.trim(), excludeId)) {
            throw new ConflictException("Товар с артикулом «" + sku.trim() + "» уже существует");
        }
    }

    private Product load(Long id) {
        return products.findById(id).orElseThrow(() -> NotFoundException.of("Товар", id));
    }

    private ProductSummaryView toSummary(Product p, String brandName, String categoryName) {
        return new ProductSummaryView(p.getId(), p.getSku(), p.getOemNumber(), p.getName(), p.getPrice(),
                p.getStockQuantity(), p.getImageUrl(), p.isActive(), p.getBrandId(), brandName,
                p.getCategoryId(), categoryName);
    }

    private ProductView toView(Product p) {
        String brandName = brands.findById(p.getBrandId()).map(Brand::getName).orElse(null);
        String categoryName = categories.findById(p.getCategoryId()).map(Category::getName).orElse(null);
        Map<Long, CarMake> makeById = makes.findAll().stream()
                .collect(Collectors.toMap(CarMake::getId, Function.identity()));
        List<CarModelView> compatible = models.findAll().stream()
                .filter(m -> p.getCompatibleModelIds().contains(m.getId()))
                .map(m -> CarModelView.from(m, makeById.get(m.getMakeId())))
                .sorted(Comparator.comparing((CarModelView v) -> v.makeName() == null ? "" : v.makeName())
                        .thenComparing(CarModelView::name))
                .toList();
        return new ProductView(p.getId(), p.getSku(), p.getOemNumber(), p.getName(), p.getPrice(),
                p.getStockQuantity(), p.getImageUrl(), p.isActive(), p.getBrandId(), brandName,
                p.getCategoryId(), categoryName, p.getDescription(), compatible, p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
