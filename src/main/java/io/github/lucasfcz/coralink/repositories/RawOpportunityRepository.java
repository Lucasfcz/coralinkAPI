package io.github.lucasfcz.coralink.repositories;

import io.github.lucasfcz.coralink.model.RawOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawOpportunityRepository extends JpaRepository<RawOpportunity, Long> {

    boolean existsByNewsUrl(String newsUrl);

    List<RawOpportunity> findByScreenedRelevantIsTrueAndBecameOpportunityIsFalse();
}