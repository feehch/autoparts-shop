package feehch.dev.autoparts.application.auth;

import feehch.dev.autoparts.application.port.out.IssuedToken;
import feehch.dev.autoparts.application.port.out.PasswordHasher;
import feehch.dev.autoparts.application.port.out.TokenProvider;
import feehch.dev.autoparts.application.port.out.TokenStore;
import feehch.dev.autoparts.application.port.out.UnitOfWork;
import feehch.dev.autoparts.application.port.out.UserRepository;
import feehch.dev.autoparts.application.security.AuthenticatedUser;
import feehch.dev.autoparts.application.security.AuthenticationFailedException;
import feehch.dev.autoparts.application.user.UserView;
import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.common.NotFoundException;
import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.domain.user.User;

import java.time.Clock;
import java.util.Set;

/**
 * Сценарии аутентификации: регистрация, вход, обновление токенов, выход.
 */
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final TokenStore tokenStore;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordHasher passwordHasher, TokenProvider tokenProvider,
                       TokenStore tokenStore, UnitOfWork unitOfWork, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.tokenStore = tokenStore;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    /**
     * Регистрирует нового покупателя и сразу выполняет вход.
     */
    public AuthResult register(RegisterCommand command) {
        User user = unitOfWork.inTransaction(() -> {
            String email = User.normalizeEmail(command.email());
            if (users.existsByEmail(email)) {
                throw new ConflictException("Пользователь с email " + email + " уже зарегистрирован");
            }
            User created = User.register(email, passwordHasher.hash(command.password()), command.firstName(),
                    command.lastName(), command.phone(), Set.of(Role.CUSTOMER), clock.instant());
            return users.save(created);
        });
        return issueTokens(user);
    }

    public AuthResult login(LoginCommand command) {
        User user = users.findByEmail(User.normalizeEmail(command.email()))
                .filter(u -> passwordHasher.matches(command.password(), u.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationFailedException("Неверный email или пароль"));
        user.ensureCanLogin();
        return issueTokens(user);
    }

    /**
     * Обменивает refresh-токен на новую пару токенов (старый refresh-токен становится недействительным).
     */
    public AuthResult refresh(String refreshToken) {
        Long userId = tokenStore.consumeRefreshToken(refreshToken)
                .orElseThrow(() -> new AuthenticationFailedException("Refresh-токен недействителен или истёк"));
        User user = users.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Пользователь не найден"));
        user.ensureCanLogin();
        return issueTokens(user);
    }

    public void logout(AuthenticatedUser currentUser, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenStore.revokeRefreshToken(refreshToken);
        }
        if (currentUser != null && currentUser.tokenId() != null) {
            tokenStore.blacklistAccessToken(currentUser.tokenId(), currentUser.tokenExpiresAt());
        }
    }

    public UserView currentUser(Long userId) {
        return users.findById(userId).map(UserView::from)
                .orElseThrow(() -> NotFoundException.of("Пользователь", userId));
    }

    private AuthResult issueTokens(User user) {
        IssuedToken accessToken = tokenProvider.issueAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken();
        tokenStore.saveRefreshToken(refreshToken, user.getId(), tokenProvider.refreshTokenTtl());
        return new AuthResult(accessToken.value(), refreshToken, TOKEN_TYPE, accessToken.ttlSeconds(),
                UserView.from(user));
    }
}
