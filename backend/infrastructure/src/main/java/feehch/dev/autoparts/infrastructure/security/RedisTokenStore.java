package feehch.dev.autoparts.infrastructure.security;

import feehch.dev.autoparts.application.port.out.TokenStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

/**
 * Хранение refresh-токенов и чёрного списка access-токенов в Redis.
 * <p>
 * Ключи:
 * <ul>
 *     <li>{@code autoparts:refresh:<sha256>} — id пользователя, TTL = срок жизни refresh-токена;</li>
 *     <li>{@code autoparts:refresh-user:<userId>} — множество хешей refresh-токенов пользователя;</li>
 *     <li>{@code autoparts:blacklist:<jti>} — отозванный access-токен, TTL = остаток срока жизни;</li>
 *     <li>{@code autoparts:revoked-before:<userId>} — момент, раньше которого выпущенные токены недействительны.</li>
 * </ul>
 * В Redis хранится не сам refresh-токен, а его SHA-256 хеш.
 */
@Component
public class RedisTokenStore implements TokenStore {

    private static final String REFRESH = "autoparts:refresh:";
    private static final String USER_REFRESH = "autoparts:refresh-user:";
    private static final String BLACKLIST = "autoparts:blacklist:";
    private static final String REVOKED_BEFORE = "autoparts:revoked-before:";

    private final StringRedisTemplate redis;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public RedisTokenStore(StringRedisTemplate redis, JwtProperties jwtProperties, Clock clock) {
        this.redis = redis;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Override
    public void saveRefreshToken(String token, Long userId, Duration ttl) {
        String hash = sha256(token);
        redis.opsForValue().set(REFRESH + hash, String.valueOf(userId), ttl);
        redis.opsForSet().add(USER_REFRESH + userId, hash);
        redis.expire(USER_REFRESH + userId, ttl);
    }

    @Override
    public Optional<Long> consumeRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String hash = sha256(token);
        String userId = redis.opsForValue().getAndDelete(REFRESH + hash);
        if (userId == null) {
            return Optional.empty();
        }
        redis.opsForSet().remove(USER_REFRESH + userId, hash);
        return Optional.of(Long.valueOf(userId));
    }

    @Override
    public void revokeRefreshToken(String token) {
        consumeRefreshToken(token);
    }

    @Override
    public void revokeAllRefreshTokens(Long userId) {
        Set<String> hashes = redis.opsForSet().members(USER_REFRESH + userId);
        if (hashes != null) {
            hashes.forEach(hash -> redis.delete(REFRESH + hash));
        }
        redis.delete(USER_REFRESH + userId);
    }

    @Override
    public void blacklistAccessToken(String tokenId, Instant expiresAt) {
        Duration ttl = Duration.between(clock.instant(), expiresAt);
        if (!ttl.isNegative() && !ttl.isZero()) {
            redis.opsForValue().set(BLACKLIST + tokenId, "1", ttl);
        }
    }

    @Override
    public boolean isAccessTokenBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST + tokenId));
    }

    @Override
    public void revokeAccessTokensIssuedBefore(Long userId, Instant moment) {
        // по истечении TTL access-токена все более ранние токены и так недействительны
        redis.opsForValue().set(REVOKED_BEFORE + userId, String.valueOf(moment.getEpochSecond()),
                jwtProperties.accessTokenTtl().plusMinutes(1));
    }

    @Override
    public Optional<Instant> accessTokensRevokedBefore(Long userId) {
        String value = redis.opsForValue().get(REVOKED_BEFORE + userId);
        return value == null ? Optional.empty() : Optional.of(Instant.ofEpochSecond(Long.parseLong(value)));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
