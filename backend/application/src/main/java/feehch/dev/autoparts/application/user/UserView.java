package feehch.dev.autoparts.application.user;

import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.domain.user.User;

import java.time.Instant;
import java.util.Set;

/**
 * Представление пользователя для внешних слоёв (без хеша пароля).
 */
public record UserView(Long id, String email, String firstName, String lastName, String phone,
                       Set<Role> roles, boolean enabled, Instant createdAt) {

    public static UserView from(User user) {
        return new UserView(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhone(), user.getRoles(), user.isEnabled(), user.getCreatedAt());
    }
}
