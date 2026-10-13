package feehch.dev.autoparts.domain.catalog;

/**
 * Варианты сортировки каталога.
 */
public enum ProductSort {
    NAME_ASC("name,asc"),
    NAME_DESC("name,desc"),
    PRICE_ASC("price,asc"),
    PRICE_DESC("price,desc"),
    NEWEST("createdAt,desc");

    private final String code;

    ProductSort(String code) {
        this.code = code;
    }

    public static ProductSort fromCode(String code) {
        if (code == null || code.isBlank()) {
            return NAME_ASC;
        }
        for (ProductSort sort : values()) {
            if (sort.code.equalsIgnoreCase(code.trim())) {
                return sort;
            }
        }
        return NAME_ASC;
    }
}
