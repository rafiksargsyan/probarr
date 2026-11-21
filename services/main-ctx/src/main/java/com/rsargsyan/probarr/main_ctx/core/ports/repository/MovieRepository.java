package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
  Optional<Movie> findByTmdbId(Long tmdbId);

  // forceScan always wins. Otherwise, a movie is due only if the usual interval/never-scanned
  // condition holds AND it isn't both old-by-release and old-by-creation with nothing found yet
  // (releaseDate IS NULL is treated as "not old" - we don't know its age, so don't skip it).
  @Query("SELECT m.id FROM Movie m WHERE m.forceScan = true " +
      "OR ((m.lastScanAt IS NULL OR m.lastScanAt < :threshold) " +
      "AND NOT (m.releaseDate IS NOT NULL AND m.releaseDate <= :releaseDateCutoff AND m.createdAt <= :createdAtCutoff))")
  List<Long> findIdsDueForScan(@Param("threshold") Instant threshold,
                               @Param("releaseDateCutoff") LocalDate releaseDateCutoff,
                               @Param("createdAtCutoff") Instant createdAtCutoff);

  @Query(value = "SELECT id FROM movie WHERE jsonb_array_length(release_candidates) > 0", nativeQuery = true)
  List<Long> findIdsWithPendingCandidates();

  @Query("SELECT m.id FROM Movie m WHERE m.tmdbId IS NOT NULL AND (m.lastEnrichedAt IS NULL OR m.lastEnrichedAt < :threshold)")
  List<Long> findIdsDueForEnrichment(@Param("threshold") Instant threshold);
}
