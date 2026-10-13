package feehch.dev.autoparts.infrastructure.persistence.adapter;

import feehch.dev.autoparts.application.port.out.UserRepository;
import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;
import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.domain.user.User;
import feehch.dev.autoparts.infrastructure.persistence.entity.UserEntity;
import feehch.dev.autoparts.infrastructure.persistence.jpa.UserJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = user.getId() == null ? new UserEntity()
                : jpa.findById(user.getId()).orElseGet(UserEntity::new);
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setPhone(user.getPhone());
        entity.setEnabled(user.isEnabled());
        entity.setCreatedAt(user.getCreatedAt());
        entity.getRoles().clear();
        entity.getRoles().addAll(user.getRoles());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Page<User> search(String query, Role role, PageQuery pageQuery) {
        Specification<UserEntity> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like)));
            }
            if (role != null) {
                predicates.add(cb.isMember(role, root.get("roles")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var result = jpa.findAll(spec, PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by("id")));
        return Page.of(result.getContent().stream().map(UserRepositoryAdapter::toDomain).toList(), pageQuery,
                result.getTotalElements());
    }

    @Override
    public List<User> findAllById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpa.findAllById(ids).stream().map(UserRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countByRole(Role role) {
        return jpa.countByRole(role);
    }

    static User toDomain(UserEntity e) {
        return new User(e.getId(), e.getEmail(), e.getPasswordHash(), e.getFirstName(), e.getLastName(),
                e.getPhone(), new HashSet<>(e.getRoles()), e.isEnabled(), e.getCreatedAt());
    }
}
