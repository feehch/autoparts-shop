package feehch.dev.autoparts.domain.common;

/**
 * Запрошенный объект предметной области не существует.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entity, Object id) {
        return new NotFoundException(entity + " с идентификатором " + id + " не найден(а)");
    }
}
