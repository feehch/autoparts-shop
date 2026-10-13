package feehch.dev.autoparts.application.port.out;

/**
 * Порт хеширования паролей.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
