package io.github.lucasfcz.coralink.modules.opportunity.repository;

import io.github.lucasfcz.coralink.modules.opportunity.model.Opportunity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

    @Override
    @NonNull
    Page<Opportunity> findAll(@NonNull Specification<Opportunity> spec, @NonNull Pageable pageable);

    @Query("SELECT COUNT(o) FROM Opportunity o WHERE o.expiresAt >= CURRENT_DATE")
    int countActiveOpportunities();

    @Query("SELECT o.type, COUNT(o) FROM Opportunity o WHERE o.expiresAt >= CURRENT_DATE GROUP BY o.type")
    java.util.List<Object[]> countActiveGroupedByType();

    @Query("SELECT o.sourceName, COUNT(o) FROM Opportunity o WHERE o.expiresAt >= CURRENT_DATE GROUP BY o.sourceName")
    java.util.List<Object[]> countActiveGroupedBySource();
}

