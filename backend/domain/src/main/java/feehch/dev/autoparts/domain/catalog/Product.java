package feehch.dev.autoparts.domain.catalog;

import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.common.Guard;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Товар — автозапчасть. Хранит остаток на складе и список совместимых моделей автомобилей.
 */
public class Product implements Serializable {

    /** Порог «заканчивается на складе». */
    public static final int LOW_STOCK_THRESHOLD = 5;

    private Long id;
    private String sku;
    private String oemNumber;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private String imageUrl;
    private boolean active;
    private Long brandId;
    private Long categoryId;
    private Set<Long> compatibleModelIds;
    private Instant createdAt;
    private Instant updatedAt;

    public Product(Long id, String sku, String oemNumber, String name, String description, BigDecimal price,
                   int stockQuantity, String imageUrl, boolean active, Long brandId, Long categoryId,
                   Set<Long> compatibleModelIds, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.sku = Guard.notBlank(sku, "артикул").toUpperCase(Locale.ROOT);
        this.oemNumber = Guard.trimToNull(oemNumber);
        this.name = Guard.notBlank(name, "название");
        this.description = Guard.trimToNull(description);
        this.price = normalizePrice(price);
        this.stockQuantity = Guard.notNegative(stockQuantity, "остаток");
        this.imageUrl = Guard.trimToNull(imageUrl);
        this.active = active;
        this.brandId = Guard.notNull(brandId, "бренд");
        this.categoryId = Guard.notNull(categoryId, "категория");
        this.compatibleModelIds = compatibleModelIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(compatibleModelIds);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(String sku, String oemNumber, String name, String description, BigDecimal price,
                                 int stockQuantity, String imageUrl, boolean active, Long brandId, Long categoryId,
                                 Set<Long> compatibleModelIds, Instant now) {
        return new Product(null, sku, oemNumber, name, description, price, stockQuantity, imageUrl, active,
                brandId, categoryId, compatibleModelIds, now, now);
    }

    public void update(String sku, String oemNumber, String name, String description, BigDecimal price,
                       int stockQuantity, String imageUrl, boolean active, Long brandId, Long categoryId,
                       Set<Long> compatibleModelIds, Instant now) {
        this.sku = Guard.notBlank(sku, "артикул").toUpperCase(Locale.ROOT);
        this.oemNumber = Guard.trimToNull(oemNumber);
        this.name = Guard.notBlank(name, "название");
        this.description = Guard.trimToNull(description);
        this.price = normalizePrice(price);
        this.stockQuantity = Guard.notNegative(stockQuantity, "остаток");
        this.imageUrl = Guard.trimToNull(imageUrl);
        this.active = active;
        this.brandId = Guard.notNull(brandId, "бренд");
        this.categoryId = Guard.notNull(categoryId, "категория");
        this.compatibleModelIds = compatibleModelIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(compatibleModelIds);
        this.updatedAt = now;
    }

    public void changeStock(int quantity, Instant now) {
        this.stockQuantity = Guard.notNegative(quantity, "остаток");
        this.updatedAt = now;
    }

    /**
     * Списывает товар со склада при оформлении заказа.
     */
    public void reserve(int quantity, Instant now) {
        Guard.positive(quantity, "количество");
        if (!active) {
            throw new ConflictException("Товар «" + name + "» недоступен для заказа");
        }
        if (quantity > stockQuantity) {
            throw new InsufficientStockException(name, quantity, stockQuantity);
        }
        stockQuantity -= quantity;
        updatedAt = now;
    }

    /**
     * Возвращает товар на склад (например, при отмене заказа).
     */
    public void release(int quantity, Instant now) {
        Guard.positive(quantity, "количество");
        stockQuantity += quantity;
        updatedAt = now;
    }

    /**
     * Проверяет, можно ли положить в корзину указанное количество товара.
     */
    public void ensureCanBeOrdered(int quantity) {
        if (!active) {
            throw new ConflictException("Товар «" + name + "» недоступен для заказа");
        }
        if (quantity > stockQuantity) {
            throw new InsufficientStockException(name, quantity, stockQuantity);
        }
    }

    public boolean isAvailable(int quantity) {
        return active && quantity <= stockQuantity;
    }

    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = now;
    }

    private static BigDecimal normalizePrice(BigDecimal price) {
        return Guard.notNegative(price, "цена").setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public String getOemNumber() {
        return oemNumber;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public Long getBrandId() {
        return brandId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Set<Long> getCompatibleModelIds() {
        return Collections.unmodifiableSet(compatibleModelIds);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
