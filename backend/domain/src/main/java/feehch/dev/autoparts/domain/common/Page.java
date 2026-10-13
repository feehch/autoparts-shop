package feehch.dev.autoparts.domain.common;

import java.util.List;
import java.util.function.Function;

/**
 * Страница результатов выборки.
 */
public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> Page<T> of(List<T> content, PageQuery query, long totalElements) {
        int totalPages = query.size() == 0 ? 0 : (int) Math.ceil((double) totalElements / query.size());
        return new Page<>(List.copyOf(content), query.page(), query.size(), totalElements, totalPages);
    }

    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new Page<>(mapped, page, size, totalElements, totalPages);
    }
}
