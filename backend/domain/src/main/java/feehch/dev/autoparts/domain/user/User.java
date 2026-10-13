package feehch.dev.autoparts.domain.user;

import feehch.dev.autoparts.domain.common.DomainException;
import feehch.dev.autoparts.domain.common.Guard;
import feehch.dev.autoparts.domain.common.PhoneNumber;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Пользователь системы (покупатель, менеджер или администратор).
 */
public class User implements Serializable {

    private Long id;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String phone;
    private Set<Role> roles;
    private boolean enabled;
    private Instant createdAt;

    public User(Long id, String email, String passwordHash, String firstName, String lastName, String phone,
                Set<Role> roles, boolean enabled, Instant createdAt) {
        this.id = id;
        this.email = normalizeEmail(email);
        this.passwordHash = Guard.notBlank(passwordHash, "пароль");
        this.firstName = Guard.notBlank(firstName, "имя");
        this.lastName = Guard.notBlank(lastName, "фамилия");
        this.phone = PhoneNumber.normalize(phone);
        this.roles = copyRoles(roles);
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    /**
     * Создаёт нового активного пользователя с указанными ролями.
     */
    public static User register(String email, String passwordHash, String firstName, String lastName,
                                String phone, Set<Role> roles, Instant now) {
        return new User(null, email, passwordHash, firstName, lastName, phone, roles, true, now);
    }

    public static String normalizeEmail(String email) {
        return Guard.notBlank(email, "email").toLowerCase(Locale.ROOT);
    }

    public void updateProfile(String firstName, String lastName, String phone) {
        this.firstName = Guard.notBlank(firstName, "имя");
        this.lastName = Guard.notBlank(lastName, "фамилия");
        this.phone = PhoneNumber.normalize(phone);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = Guard.notBlank(passwordHash, "пароль");
    }

    public void changeRoles(Set<Role> roles) {
        this.roles = copyRoles(roles);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Проверка с учётом иерархии ролей.
     */
    public boolean hasRole(Role role) {
        return roles.stream().anyMatch(r -> r.impliedRoles().contains(role));
    }

    public void ensureCanLogin() {
        if (!enabled) {
            throw new AccountDisabledException();
        }
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    private static Set<Role> copyRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new DomainException("У пользователя должна быть хотя бы одна роль");
        }
        return EnumSet.copyOf(roles);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Учётная запись заблокирована администратором.
     */
    public static class AccountDisabledException extends DomainException {
        public AccountDisabledException() {
            super("Учётная запись заблокирована");
        }
    }
}
