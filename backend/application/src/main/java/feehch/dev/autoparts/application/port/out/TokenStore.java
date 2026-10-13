package feehch.dev.autoparts.application.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Порт хранилища сессионных данных токенов (реализуется в Redis).
 */
public interface TokenStore {

    void saveRefreshToken(String token, Long userId, Duration ttl);

    /**
     * Атомарно извлекает и удаляет refresh-токен.
     *
     * @return идентификатор пользователя, если токен действителен
     */
    Optional<Long> consumeRefreshToken(String token);

    void revokeRefreshToken(String token);

    void revokeAllRefreshTokens(Long userId);

    /**
     * Помещает access-токен в чёрный список до момента истечения его срока.
     */
    void blacklistAccessToken(String tokenId, Instant expiresAt);

    boolean isAccessTokenBlacklisted(String tokenId);

    /**
     * Делает недействительными все access-токены пользователя, выпущенные до указанного момента
     * (используется при смене ролей и блокировке).
     */
    void revokeAccessTokensIssuedBefore(Long userId, Instant moment);

    Optional<Instant> accessTokensRevokedBefore(Long userId);
}
