package feehch.dev.autoparts.bootstrap;

import feehch.dev.autoparts.application.port.out.PasswordHasher;
import feehch.dev.autoparts.application.port.out.UserRepository;
import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.domain.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.EnumSet;
import java.util.Set;

/**
 * Создаёт демонстрационные учётные записи при первом запуске (если их ещё нет).
 * Пароли хешируются тем же {@link PasswordHasher}, что и при регистрации.
 * Отключается свойством {@code app.seed.demo-users=false}.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo-users", havingValue = "true", matchIfMissing = true)
public class DemoUsersSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUsersSeeder.class);

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public DemoUsersSeeder(UserRepository users, PasswordHasher passwordHasher, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("admin@autoparts.local", "Admin123!", "Анна", "Администраторова", "+375290000001",
                EnumSet.of(Role.ADMIN, Role.MANAGER, Role.CUSTOMER));
        seed("manager@autoparts.local", "Manager123!", "Максим", "Менеджеров", "+375290000002",
                EnumSet.of(Role.MANAGER, Role.CUSTOMER));
        seed("customer@autoparts.local", "Customer123!", "Иван", "Покупателев", "+375290000003",
                EnumSet.of(Role.CUSTOMER));
    }

    private void seed(String email, String password, String firstName, String lastName, String phone,
                      Set<Role> roles) {
        if (users.existsByEmail(email)) {
            return;
        }
        users.save(User.register(email, passwordHasher.hash(password), firstName, lastName, phone, roles,
                clock.instant()));
        log.info("Создан демонстрационный пользователь {} с ролями {}", email, roles);
    }
}
