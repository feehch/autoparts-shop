package feehch.dev.autoparts.application.port.out;

import java.time.Instant;

/**
 * Выпущенный access-токен.
 *
 * @param value     сам токен
 * @param id        уникальный идентификатор токена (jti)
 * @param expiresAt момент истечения срока действия
 * @param ttlSeconds время жизни в секундах
 */
public record IssuedToken(String value, String id, Instant expiresAt, long ttlSeconds) {
}
