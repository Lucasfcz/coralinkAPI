package io.github.lucasfcz.coralink.repositories;

import io.github.lucasfcz.coralink.model.Opportunity;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

    @Override
    @EntityGraph(attributePaths = {"targetCourseAudiences"})
    @NonNull
    Page<Opportunity> findAll(@NonNull Specification<Opportunity> spec, @NonNull Pageable pageable);

    @Query("""
        SELECT COUNT(o) FROM Opportunity o
        WHERE o.isActive = true
          AND (
            (o.startDate IS NOT NULL AND (o.startDate >= :today OR (o.endDate IS NOT NULL AND o.endDate >= :today)))
            OR (o.startDate IS NULL AND o.createdAt > :cutoff45d)
          )
    """)
    int countActiveOpportunities(@Param("today") LocalDate today, @Param("cutoff45d") LocalDateTime cutoff45d);
}
