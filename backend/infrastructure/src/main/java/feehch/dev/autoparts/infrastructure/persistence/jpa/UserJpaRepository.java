package feehch.dev.autoparts.infrastructure.persistence.jpa;

import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select count(u) from UserEntity u join u.roles r where r = :role")
    long countByRole(@Param("role") Role role);
}
