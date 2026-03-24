package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.ReleaseCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseCandidateRepository extends JpaRepository<ReleaseCandidate, Long> {
  List<ReleaseCandidate> findByMovieId(Long movieId);
  List<ReleaseCandidate> findByEpisodeId(Long episodeId);
}
