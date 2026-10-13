package feehch.dev.autoparts.application.port.out;

import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;
import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.domain.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Порт хранилища пользователей.
 */
public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);

    /**
     * Поиск по email/имени/фамилии и роли (параметры необязательны).
     */
    Page<User> search(String query, Role role, PageQuery pageQuery);

    List<User> findAllById(Collection<Long> ids);

    long countByRole(Role role);
}
