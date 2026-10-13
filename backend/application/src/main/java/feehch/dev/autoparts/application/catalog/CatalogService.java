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
import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.common.NotFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Справочники каталога: категории, бренды, марки и модели автомобилей.
 */
public class CatalogService {

    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final CarMakeRepository makes;
    private final CarModelRepository models;
    private final ProductRepository products;
    private final UnitOfWork unitOfWork;

    public CatalogService(CategoryRepository categories, BrandRepository brands, CarMakeRepository makes,
                          CarModelRepository models, ProductRepository products, UnitOfWork unitOfWork) {
        this.categories = categories;
        this.brands = brands;
        this.makes = makes;
        this.models = models;
        this.products = products;
        this.unitOfWork = unitOfWork;
    }

    // ---------- Категории ----------

    public List<CategoryView> listCategories() {
        return categories.findAll().stream()
                .sorted(Comparator.comparing(Category::getName))
                .map(CategoryView::from).toList();
    }

    public CategoryView getCategory(Long id) {
        return CategoryView.from(loadCategory(id));
    }

    public CategoryView createCategory(CategoryCommand command) {
        return unitOfWork.inTransaction(() -> {
            ensureParentExists(command.parentId());
            Category category = new Category(null, command.name(), command.slug(), command.description(),
                    command.parentId());
            ensureSlugUnique(category.getSlug(), null);
            return CategoryView.from(categories.save(category));
        });
    }

    public CategoryView updateCategory(Long id, CategoryCommand command) {
        return unitOfWork.inTransaction(() -> {
            Category category = loadCategory(id);
            ensureParentExists(command.parentId());
            category.update(command.name(), command.slug(), command.description(), command.parentId());
            category.ensureNoCycle(categories.findAll());
            ensureSlugUnique(category.getSlug(), id);
            return CategoryView.from(categories.save(category));
        });
    }

    public void deleteCategory(Long id) {
        unitOfWork.inTransaction(() -> {
            loadCategory(id);
            if (categories.hasChildren(id)) {
                throw new ConflictException("Нельзя удалить категорию, у которой есть подкатегории");
            }
            if (products.existsByCategoryId(id)) {
                throw new ConflictException("Нельзя удалить категорию, в которой есть товары");
            }
            categories.deleteById(id);
        });
    }

    // ---------- Бренды ----------

    public List<BrandView> listBrands() {
        return brands.findAll().stream()
                .sorted(Comparator.comparing(Brand::getName, String.CASE_INSENSITIVE_ORDER))
                .map(BrandView::from).toList();
    }

    public BrandView getBrand(Long id) {
        return BrandView.from(loadBrand(id));
    }

    public BrandView createBrand(BrandCommand command) {
        return unitOfWork.inTransaction(() -> {
            Brand brand = new Brand(null, command.name(), command.country(), command.description());
            if (brands.existsByName(brand.getName(), null)) {
                throw new ConflictException("Бренд «" + brand.getName() + "» уже существует");
            }
            return BrandView.from(brands.save(brand));
        });
    }

    public BrandView updateBrand(Long id, BrandCommand command) {
        return unitOfWork.inTransaction(() -> {
            Brand brand = loadBrand(id);
            brand.update(command.name(), command.country(), command.description());
            if (brands.existsByName(brand.getName(), id)) {
                throw new ConflictException("Бренд «" + brand.getName() + "» уже существует");
            }
            return BrandView.from(brands.save(brand));
        });
    }

    public void deleteBrand(Long id) {
        unitOfWork.inTransaction(() -> {
            loadBrand(id);
            if (products.existsByBrandId(id)) {
                throw new ConflictException("Нельзя удалить бренд, у которого есть товары");
            }
            brands.deleteById(id);
        });
    }

    // ---------- Марки автомобилей ----------

    public List<CarMakeView> listMakes() {
        return makes.findAll().stream()
                .sorted(Comparator.comparing(CarMake::getName, String.CASE_INSENSITIVE_ORDER))
                .map(CarMakeView::from).toList();
    }

    public CarMakeView createMake(CarMakeCommand command) {
        return unitOfWork.inTransaction(() -> {
            CarMake make = new CarMake(null, command.name(), command.country());
            if (makes.existsByName(make.getName(), null)) {
                throw new ConflictException("Марка «" + make.getName() + "» уже существует");
            }
            return CarMakeView.from(makes.save(make));
        });
    }

    public CarMakeView updateMake(Long id, CarMakeCommand command) {
        return unitOfWork.inTransaction(() -> {
            CarMake make = loadMake(id);
            make.update(command.name(), command.country());
            if (makes.existsByName(make.getName(), id)) {
                throw new ConflictException("Марка «" + make.getName() + "» уже существует");
            }
            return CarMakeView.from(makes.save(make));
        });
    }

    public void deleteMake(Long id) {
        unitOfWork.inTransaction(() -> {
            loadMake(id);
            if (models.existsByMakeId(id)) {
                throw new ConflictException("Нельзя удалить марку, у которой есть модели");
            }
            makes.deleteById(id);
        });
    }

    // ---------- Модели автомобилей ----------

    public List<CarModelView> listModels() {
        return toModelViews(models.findAll());
    }

    public List<CarModelView> listModelsByMake(Long makeId) {
        loadMake(makeId);
        return toModelViews(models.findByMakeId(makeId));
    }

    public CarModelView createModel(CarModelCommand command) {
        return unitOfWork.inTransaction(() -> {
            CarMake make = loadMake(command.makeId());
            CarModel model = new CarModel(null, command.makeId(), command.name(), command.yearFrom(),
                    command.yearTo());
            return CarModelView.from(models.save(model), make);
        });
    }

    public CarModelView updateModel(Long id, CarModelCommand command) {
        return unitOfWork.inTransaction(() -> {
            CarModel model = models.findById(id).orElseThrow(() -> NotFoundException.of("Модель автомобиля", id));
            CarMake make = loadMake(command.makeId());
            model.update(command.makeId(), command.name(), command.yearFrom(), command.yearTo());
            return CarModelView.from(models.save(model), make);
        });
    }

    public void deleteModel(Long id) {
        unitOfWork.inTransaction(() -> {
            models.findById(id).orElseThrow(() -> NotFoundException.of("Модель автомобиля", id));
            models.deleteById(id);
        });
    }

    /**
     * Преобразует модели в представления, подставляя название марки и сортируя по марке, модели и году.
     */
    List<CarModelView> toModelViews(List<CarModel> list) {
        Map<Long, CarMake> makeById = makes.findAll().stream()
                .collect(Collectors.toMap(CarMake::getId, Function.identity()));
        return list.stream()
                .map(m -> CarModelView.from(m, makeById.get(m.getMakeId())))
                .sorted(Comparator.comparing((CarModelView v) -> v.makeName() == null ? "" : v.makeName())
                        .thenComparing(CarModelView::name)
                        .thenComparing(CarModelView::yearFrom))
                .toList();
    }

    private void ensureParentExists(Long parentId) {
        if (parentId != null) {
            loadCategory(parentId);
        }
    }

    private void ensureSlugUnique(String slug, Long excludeId) {
        if (categories.existsBySlug(slug, excludeId)) {
            throw new ConflictException("Категория со slug «" + slug + "» уже существует");
        }
    }

    private Category loadCategory(Long id) {
        return categories.findById(id).orElseThrow(() -> NotFoundException.of("Категория", id));
    }

    private Brand loadBrand(Long id) {
        return brands.findById(id).orElseThrow(() -> NotFoundException.of("Бренд", id));
    }

    private CarMake loadMake(Long id) {
        return makes.findById(id).orElseThrow(() -> NotFoundException.of("Марка автомобиля", id));
    }
}
