package io.github.lucasfcz.coralink.modules.auth.repository;

import io.github.lucasfcz.coralink.modules.auth.model.Role;
import io.github.lucasfcz.coralink.modules.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    long countByRole(Role role);
}
