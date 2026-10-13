package feehch.dev.autoparts.application.security;

/**
 * У пользователя недостаточно прав для операции.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
