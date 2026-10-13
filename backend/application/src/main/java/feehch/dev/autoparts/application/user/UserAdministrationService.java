package feehch.dev.autoparts.application.user;

import feehch.dev.autoparts.application.port.out.PasswordHasher;
import feehch.dev.autoparts.application.port.out.TokenStore;
import feehch.dev.autoparts.application.port.out.UnitOfWork;
import feehch.dev.autoparts.application.port.out.UserRepository;
import feehch.dev.autoparts.application.security.AuthenticatedUser;
import feehch.dev.autoparts.domain.common.ConflictException;
import feehch.dev.autoparts.domain.common.NotFoundException;
import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;
import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.domain.user.User;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Администрирование пользователей: создание, распределение ролей, блокировка.
 */
public class UserAdministrationService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenStore tokenStore;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public UserAdministrationService(UserRepository users, PasswordHasher passwordHasher, TokenStore tokenStore,
                                     UnitOfWork unitOfWork, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenStore = tokenStore;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    public Page<UserView> search(String query, Role role, PageQuery pageQuery) {
        return users.search(query, role, pageQuery).map(UserView::from);
    }

    public UserView get(Long id) {
        return UserView.from(load(id));
    }

    public List<RoleView> roles() {
        return Arrays.stream(Role.values()).map(RoleView::from).toList();
    }

    public UserView create(CreateUserCommand command) {
        return unitOfWork.inTransaction(() -> {
            String email = User.normalizeEmail(command.email());
            if (users.existsByEmail(email)) {
                throw new ConflictException("Пользователь с email " + email + " уже существует");
            }
            User user = User.register(email, passwordHasher.hash(command.password()), command.firstName(),
                    command.lastName(), command.phone(), command.roles(), clock.instant());
            return UserView.from(users.save(user));
        });
    }

    /**
     * Назначает пользователю набор ролей. Действующие access-токены пользователя отзываются,
     * чтобы новые права вступили в силу при следующем обновлении токена.
     */
    public UserView changeRoles(AuthenticatedUser actor, Long userId, Set<Role> roles) {
        UserView result = unitOfWork.inTransaction(() -> {
            User user = load(userId);
            if (user.getId().equals(actor.id()) && (roles == null || !roles.contains(Role.ADMIN))) {
                throw new ConflictException("Нельзя снять роль администратора с самого себя");
            }
            user.changeRoles(roles);
            return UserView.from(users.save(user));
        });
        tokenStore.revokeAccessTokensIssuedBefore(userId, clock.instant());
        return result;
    }

    /**
     * Блокирует или разблокирует пользователя. При блокировке все его токены отзываются.
     */
    public UserView setEnabled(AuthenticatedUser actor, Long userId, boolean enabled) {
        if (userId.equals(actor.id()) && !enabled) {
            throw new ConflictException("Нельзя заблокировать самого себя");
        }
        UserView result = unitOfWork.inTransaction(() -> {
            User user = load(userId);
            user.setEnabled(enabled);
            return UserView.from(users.save(user));
        });
        if (!enabled) {
            tokenStore.revokeAllRefreshTokens(userId);
            tokenStore.revokeAccessTokensIssuedBefore(userId, clock.instant());
        }
        return result;
    }

    private User load(Long id) {
        return users.findById(id).orElseThrow(() -> NotFoundException.of("Пользователь", id));
    }
}
