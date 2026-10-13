package feehch.dev.autoparts.domain.catalog;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Критерии поиска товаров в каталоге.
 *
 * @param query           строка поиска по названию, артикулу и OEM-номеру
 * @param categoryIds     категории (уже с учётом подкатегорий); {@code null} — без фильтра
 * @param brandId         бренд
 * @param makeId          марка автомобиля
 * @param modelId         модель автомобиля
 * @param minPrice        минимальная цена
 * @param maxPrice        максимальная цена
 * @param inStockOnly     только товары в наличии
 * @param includeInactive включать неактивные товары
 * @param sort            порядок сортировки
 */
public record ProductSearchCriteria(
        String query,
        Set<Long> categoryIds,
        Long brandId,
        Long makeId,
        Long modelId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean inStockOnly,
        boolean includeInactive,
        ProductSort sort
) {
    public ProductSearchCriteria {
        sort = sort == null ? ProductSort.NAME_ASC : sort;
    }
}
