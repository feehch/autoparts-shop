package feehch.dev.autoparts.application.port.out;

import feehch.dev.autoparts.domain.user.User;

import java.time.Duration;

/**
 * Порт выпуска токенов доступа.
 */
public interface TokenProvider {

    IssuedToken issueAccessToken(User user);

    /**
     * Генерирует непрозрачный (opaque) refresh-токен.
     */
    String generateRefreshToken();

    Duration refreshTokenTtl();
}
