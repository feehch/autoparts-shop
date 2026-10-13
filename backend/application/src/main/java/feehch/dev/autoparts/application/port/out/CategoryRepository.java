package feehch.dev.autoparts.application.port.out;

import feehch.dev.autoparts.domain.catalog.Category;

import java.util.List;
import java.util.Optional;

/**
 * Порт хранилища категорий каталога.
 */
public interface CategoryRepository {

    List<Category> findAll();

    Optional<Category> findById(Long id);

    Category save(Category category);

    void deleteById(Long id);

    boolean existsBySlug(String slug, Long excludeId);

    boolean hasChildren(Long id);
}
