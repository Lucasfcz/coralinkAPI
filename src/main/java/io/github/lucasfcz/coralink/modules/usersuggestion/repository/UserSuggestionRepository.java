package io.github.lucasfcz.coralink.modules.usersuggestion.repository;

import io.github.lucasfcz.coralink.modules.usersuggestion.model.SuggestionType;
import io.github.lucasfcz.coralink.modules.usersuggestion.model.UserSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSuggestionRepository extends JpaRepository<UserSuggestion, Long> {
    Page<UserSuggestion> findAllByType(SuggestionType type, Pageable pageable);
}
