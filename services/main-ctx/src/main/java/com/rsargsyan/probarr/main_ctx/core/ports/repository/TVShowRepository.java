package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.TVShow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TVShowRepository extends JpaRepository<TVShow, Long> {
  Optional<TVShow> findByImdbId(String imdbId);
  Optional<TVShow> findByTvdbId(Long tvdbId);
  Optional<TVShow> findByTmdbId(Long tmdbId);

  @Query("SELECT t.id FROM TVShow t WHERE t.tmdbId IS NOT NULL AND (t.lastEnrichedAt IS NULL OR t.lastEnrichedAt < :threshold)")
  List<Long> findIdsDueForEnrichment(@Param("threshold") Instant threshold);
}
