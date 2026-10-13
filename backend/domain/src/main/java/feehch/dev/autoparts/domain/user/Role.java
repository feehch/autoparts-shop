package feehch.dev.autoparts.domain.user;

import java.util.EnumSet;
import java.util.Set;

/**
 * Роль пользователя. Роли образуют иерархию: ADMIN включает права MANAGER, MANAGER — права CUSTOMER.
 */
public enum Role {
    CUSTOMER("Покупатель"),
    MANAGER("Менеджер"),
    ADMIN("Администратор");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    /**
     * Все роли, права которых включены в данную роль (включая её саму).
     */
    public Set<Role> impliedRoles() {
        return switch (this) {
            case ADMIN -> EnumSet.of(ADMIN, MANAGER, CUSTOMER);
            case MANAGER -> EnumSet.of(MANAGER, CUSTOMER);
            case CUSTOMER -> EnumSet.of(CUSTOMER);
        };
    }
}
