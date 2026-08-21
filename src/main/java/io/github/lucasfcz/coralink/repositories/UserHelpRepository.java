package io.github.lucasfcz.coralink.repositories;

import io.github.lucasfcz.coralink.enums.SuggestionType;
import io.github.lucasfcz.coralink.model.UserHelp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserHelpRepository extends JpaRepository<UserHelp, Long> {
    Page<UserHelp> findAllByType(SuggestionType type, Pageable pageable);
}
