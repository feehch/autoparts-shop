package feehch.dev.autoparts.domain.common;

/**
 * Базовое исключение нарушения бизнес-правила предметной области.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
