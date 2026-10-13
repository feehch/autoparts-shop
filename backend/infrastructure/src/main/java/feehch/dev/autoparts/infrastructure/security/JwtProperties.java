package feehch.dev.autoparts.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Настройки JWT ({@code app.jwt.*}).
 *
 * @param secret          секрет для подписи HMAC-SHA256 (не короче 32 байт)
 * @param issuer          издатель токенов
 * @param accessTokenTtl  время жизни access-токена
 * @param refreshTokenTtl время жизни refresh-токена
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret должен содержать не менее 32 байт");
        }
        issuer = issuer == null ? "autoparts" : issuer;
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(7) : refreshTokenTtl;
    }
}
