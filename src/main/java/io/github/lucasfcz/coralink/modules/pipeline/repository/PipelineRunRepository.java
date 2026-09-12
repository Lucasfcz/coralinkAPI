package io.github.lucasfcz.coralink.modules.pipeline.repository;

import io.github.lucasfcz.coralink.modules.pipeline.enums.PipelineStatus;
import io.github.lucasfcz.coralink.modules.pipeline.model.PipelineRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PipelineRunRepository extends JpaRepository<PipelineRun, Long> {

    Optional<PipelineRun> findTopByOrderByStartedAtDesc();

    Page<PipelineRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
