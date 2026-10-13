package feehch.dev.autoparts.application.security;

/**
 * Неверные учётные данные или недействительный токен.
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
