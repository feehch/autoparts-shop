package feehch.dev.autoparts.domain.common;

import java.util.Set;

/**
 * Белорусский номер телефона в международном формате {@code +375XXXXXXXXX}.
 * <p>
 * Принимаются варианты записи с пробелами, дефисами и скобками, а также внутренний формат
 * с префиксом {@code 80} ({@code 8 029 123-45-67}). Код после 375 — код мобильного оператора
 * (25 life, 29 (A1 / МТС), 33 (МТС), 44 (A1)) или код города (17 — Минск, 15x, 16x, 21x–23x — областные).
 */
public final class PhoneNumber {

    public static final String COUNTRY_CODE = "+375";

    private static final Set<String> AREA_CODES = Set.of("17", "25", "29", "33", "44", "15", "16", "21", "22", "23");

    private PhoneNumber() {
    }

    /**
     * Приводит номер к виду {@code +375XXXXXXXXX}.
     *
     * @return нормализованный номер или {@code null}, если номер не указан
     * @throws DomainException если номер не является белорусским
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("[\\s\\-()]", "");
        if (!digits.matches("\\+?\\d+")) {
            throw invalid();
        }
        digits = digits.startsWith("+") ? digits.substring(1) : digits;
        if (digits.startsWith("80") && digits.length() == 11) {
            digits = "375" + digits.substring(2);
        }
        if (!digits.startsWith("375") || digits.length() != 12 || !AREA_CODES.contains(digits.substring(3, 5))) {
            throw invalid();
        }
        return "+" + digits;
    }

    public static boolean isValid(String raw) {
        try {
            return normalize(raw) != null;
        } catch (DomainException e) {
            return false;
        }
    }

    private static DomainException invalid() {
        return new DomainException("Укажите белорусский номер телефона в формате +375 (XX) XXX-XX-XX");
    }
}
