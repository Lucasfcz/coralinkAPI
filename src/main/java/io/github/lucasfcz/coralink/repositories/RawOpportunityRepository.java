package io.github.lucasfcz.coralink.repositories;

import io.github.lucasfcz.coralink.model.RawOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RawOpportunityRepository extends JpaRepository<RawOpportunity, Long> {

    List<RawOpportunity> findAllByNewsUrlIn(Collection<String> newsUrls);

    List<RawOpportunity> findByScreenedRelevantIsTrueAndBecameOpportunityIsFalse();

    List<RawOpportunity> findByScreenedRelevantIsNull();
}
