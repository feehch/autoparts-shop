package feehch.dev.autoparts.domain;

import feehch.dev.autoparts.domain.catalog.Category;
import feehch.dev.autoparts.domain.common.DomainException;
import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryAndUserTest {

    @Test
    void slugIsTransliterated() {
        assertThat(Category.slugify("Тормозные колодки")).isEqualTo("tormoznye-kolodki");
        assertThat(Category.slugify("  Oil  Filters! ")).isEqualTo("oil-filters");
    }

    @Test
    void descendantsAreCollectedRecursively() {
        List<Category> all = List.of(
                new Category(1L, "A", "a", null, null),
                new Category(2L, "B", "b", null, 1L),
                new Category(3L, "C", "c", null, 2L),
                new Category(4L, "D", "d", null, null));

        assertThat(Category.withDescendants(1L, all)).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void cycleInCategoryTreeIsRejected() {
        Category root = new Category(1L, "A", "a", null, null);
        Category child = new Category(2L, "B", "b", null, 1L);
        root.update("A", "a", null, 2L);

        assertThatThrownBy(() -> root.ensureNoCycle(List.of(root, child))).isInstanceOf(DomainException.class);
    }

    @Test
    void roleHierarchy() {
        User admin = User.register("ADMIN@Mail.ru", "hash", "A", "B", null, Set.of(Role.ADMIN), Instant.now());

        assertThat(admin.getEmail()).isEqualTo("admin@mail.ru");
        assertThat(admin.hasRole(Role.MANAGER)).isTrue();
        assertThat(admin.hasRole(Role.CUSTOMER)).isTrue();

        User customer = User.register("c@mail.ru", "hash", "A", "B", null, Set.of(Role.CUSTOMER), Instant.now());
        assertThat(customer.hasRole(Role.MANAGER)).isFalse();
    }

    @Test
    void userMustHaveAtLeastOneRole() {
        User user = User.register("c@mail.ru", "hash", "A", "B", null, Set.of(Role.CUSTOMER), Instant.now());

        assertThatThrownBy(() -> user.changeRoles(Set.of())).isInstanceOf(DomainException.class);
    }

    @Test
    void disabledUserCannotLogin() {
        User user = User.register("c@mail.ru", "hash", "A", "B", null, Set.of(Role.CUSTOMER), Instant.now());
        user.setEnabled(false);

        assertThatThrownBy(user::ensureCanLogin).isInstanceOf(User.AccountDisabledException.class);
    }
}
