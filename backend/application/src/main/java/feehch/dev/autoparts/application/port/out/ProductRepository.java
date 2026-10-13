package feehch.dev.autoparts.application.port.out;

import feehch.dev.autoparts.domain.catalog.Product;
import feehch.dev.autoparts.domain.catalog.ProductSearchCriteria;
import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Порт хранилища товаров.
 */
public interface ProductRepository {

    Optional<Product> findById(Long id);

    List<Product> findAllById(Collection<Long> ids);

    /**
     * Загружает товары с блокировкой строк до конца транзакции (защита остатков от гонок при оформлении заказа).
     */
    List<Product> findAllByIdForUpdate(Collection<Long> ids);

    Product save(Product product);

    Page<Product> search(ProductSearchCriteria criteria, PageQuery pageQuery);

    boolean existsBySku(String sku, Long excludeId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByBrandId(Long brandId);

    long count();

    long countActive();

    long countActiveWithStockBelow(int threshold);
}
