package io.github.lucasfcz.coralink.repositories;

import io.github.lucasfcz.coralink.enums.OpportunityType;
import io.github.lucasfcz.coralink.model.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    Page<Opportunity> findByType(OpportunityType type, Pageable pageable);
}