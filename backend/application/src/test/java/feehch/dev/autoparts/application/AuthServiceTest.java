package feehch.dev.autoparts.application;

import feehch.dev.autoparts.application.auth.AuthResult;
import feehch.dev.autoparts.application.auth.AuthService;
import feehch.dev.autoparts.application.auth.LoginCommand;
import feehch.dev.autoparts.application.auth.RegisterCommand;
import feehch.dev.autoparts.application.port.out.IssuedToken;
import feehch.dev.autoparts.application.port.out.PasswordHasher;
import feehch.dev.autoparts.application.port.out.TokenProvider;
import feehch.dev.autoparts.application.port.out.TokenStore;
import feehch.dev.autoparts.application.port.out.UserRepository;
import feehch.dev.autoparts.application.security.AuthenticationFailedException;
import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");

    @Mock
    private UserRepository users;
    @Mock
    private PasswordHasher hasher;
    @Mock
    private TokenProvider tokenProvider;
    @Mock
    private TokenStore tokenStore;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, hasher, tokenProvider, tokenStore, new DirectUnitOfWork(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(tokenProvider.issueAccessToken(any()))
                .thenReturn(new IssuedToken("access", "jti", NOW.plusSeconds(900), 900));
        lenient().when(tokenProvider.generateRefreshToken()).thenReturn("refresh");
        lenient().when(tokenProvider.refreshTokenTtl()).thenReturn(Duration.ofDays(7));
    }

    private User existingUser(boolean enabled) {
        User user = new User(5L, "user@mail.ru", "HASH", "Иван", "Петров", null, Set.of(Role.CUSTOMER),
                enabled, NOW);
        return user;
    }

    @Test
    void registerCreatesCustomerAndIssuesTokens() {
        when(users.existsByEmail("new@mail.ru")).thenReturn(false);
        when(hasher.hash("password1")).thenReturn("HASH");
        when(users.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        AuthResult result = service.register(new RegisterCommand("New@Mail.ru", "password1", "Иван", "Петров", null));

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        assertThat(result.user().roles()).containsExactly(Role.CUSTOMER);
        assertThat(result.user().email()).isEqualTo("new@mail.ru");
        verify(tokenStore).saveRefreshToken("refresh", 1L, Duration.ofDays(7));
    }

    @Test
    void registerWithExistingEmailFails() {
        when(users.existsByEmail("user@mail.ru")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterCommand("user@mail.ru", "password1", "И", "П", null)))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    @Test
    void loginWithWrongPasswordFails() {
        when(users.findByEmail("user@mail.ru")).thenReturn(Optional.of(existingUser(true)));
        when(hasher.matches("wrong", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("user@mail.ru", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void disabledUserCannotLogin() {
        when(users.findByEmail("user@mail.ru")).thenReturn(Optional.of(existingUser(false)));
        when(hasher.matches("secret", "HASH")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginCommand("user@mail.ru", "secret")))
                .isInstanceOf(User.AccountDisabledException.class);
    }

    @Test
    void refreshRotatesToken() {
        when(tokenStore.consumeRefreshToken("old")).thenReturn(Optional.of(5L));
        when(users.findById(5L)).thenReturn(Optional.of(existingUser(true)));

        AuthResult result = service.refresh("old");

        assertThat(result.refreshToken()).isEqualTo("refresh");
        verify(tokenStore).saveRefreshToken(eq("refresh"), eq(5L), any());
    }

    @Test
    void refreshWithUnknownTokenFails() {
        when(tokenStore.consumeRefreshToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("unknown")).isInstanceOf(AuthenticationFailedException.class);
    }
}
