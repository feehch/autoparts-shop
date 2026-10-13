package feehch.dev.autoparts.application.security;

import feehch.dev.autoparts.domain.user.Role;

import java.time.Instant;
import java.util.Set;

/**
 * Текущий аутентифицированный пользователь (данные из access-токена).
 */
public record AuthenticatedUser(Long id, String email, Set<Role> roles, String tokenId, Instant tokenExpiresAt) {

    public AuthenticatedUser {
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(Role role) {
        return roles.stream().anyMatch(r -> r.impliedRoles().contains(role));
    }
}
