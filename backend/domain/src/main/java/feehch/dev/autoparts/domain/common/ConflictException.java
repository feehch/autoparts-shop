package feehch.dev.autoparts.domain.common;

/**
 * Операция противоречит текущему состоянию системы (дубликат, недопустимый переход и т.п.).
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
