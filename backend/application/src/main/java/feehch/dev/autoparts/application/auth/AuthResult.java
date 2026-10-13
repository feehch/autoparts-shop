package feehch.dev.autoparts.application.auth;

import feehch.dev.autoparts.application.user.UserView;

/**
 * Результат успешной аутентификации — пара токенов и данные пользователя.
 */
public record AuthResult(String accessToken, String refreshToken, String tokenType, long expiresIn, UserView user) {
}
