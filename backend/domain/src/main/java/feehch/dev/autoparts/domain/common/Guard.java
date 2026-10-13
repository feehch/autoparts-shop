package feehch.dev.autoparts.domain.common;

import java.math.BigDecimal;

/**
 * Проверки инвариантов сущностей.
 */
public final class Guard {

    private Guard() {
    }

    public static String notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException("Поле «" + field + "» не может быть пустым");
        }
        return value.trim();
    }

    public static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new DomainException("Поле «" + field + "» обязательно");
        }
        return value;
    }

    public static int notNegative(int value, String field) {
        if (value < 0) {
            throw new DomainException("Поле «" + field + "» не может быть отрицательным");
        }
        return value;
    }

    public static int positive(int value, String field) {
        if (value <= 0) {
            throw new DomainException("Поле «" + field + "» должно быть больше нуля");
        }
        return value;
    }

    public static BigDecimal notNegative(BigDecimal value, String field) {
        notNull(value, field);
        if (value.signum() < 0) {
            throw new DomainException("Поле «" + field + "» не может быть отрицательным");
        }
        return value;
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
