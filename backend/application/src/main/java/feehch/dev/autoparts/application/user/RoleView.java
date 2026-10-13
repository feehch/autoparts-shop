package feehch.dev.autoparts.application.user;

import feehch.dev.autoparts.domain.user.Role;

/**
 * Описание роли.
 */
public record RoleView(String name, String description) {

    public static RoleView from(Role role) {
        return new RoleView(role.name(), role.description());
    }
}
