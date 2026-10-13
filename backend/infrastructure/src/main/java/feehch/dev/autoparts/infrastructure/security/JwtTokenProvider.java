package feehch.dev.autoparts.infrastructure.security;

import feehch.dev.autoparts.application.port.out.IssuedToken;
import feehch.dev.autoparts.application.port.out.TokenProvider;
import feehch.dev.autoparts.application.security.AuthenticatedUser;
import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.domain.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Выпуск и проверка JWT access-токенов (HS256), генерация refresh-токенов.
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties properties;
    private final SecretKey key;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    @Override
    public IssuedToken issueAccessToken(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        String id = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(id)
                .issuer(properties.issuer())
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLES, user.getRoles().stream().map(Role::name).sorted().toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, id, expiresAt, properties.accessTokenTtl().toSeconds());
    }

    @Override
    public String generateRefreshToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public Duration refreshTokenTtl() {
        return properties.refreshTokenTtl();
    }

    public Duration accessTokenTtl() {
        return properties.accessTokenTtl();
    }

    /**
     * Проверяет подпись, издателя и срок действия токена.
     *
     * @return данные пользователя и момент выпуска токена, либо пусто, если токен недействителен
     */
    public Optional<ParsedToken> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Set<Role> roles = EnumSet.noneOf(Role.class);
            List<?> rawRoles = claims.get(CLAIM_ROLES, List.class);
            if (rawRoles != null) {
                rawRoles.forEach(r -> roles.add(Role.valueOf(String.valueOf(r))));
            }
            AuthenticatedUser user = new AuthenticatedUser(Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class), roles, claims.getId(),
                    claims.getExpiration().toInstant());
            return Optional.of(new ParsedToken(user, claims.getIssuedAt().toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record ParsedToken(AuthenticatedUser user, Instant issuedAt) {
    }
}
