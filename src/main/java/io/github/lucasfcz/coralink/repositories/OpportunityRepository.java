package io.github.lucasfcz.coralink.repositories;

import io.github.lucasfcz.coralink.model.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

    Page<Opportunity> findOpportunitiesByTitleContainsIgnoreCase(String title, Pageable pageable);

    int countAllByStartDateAfter(LocalDate date);
}
