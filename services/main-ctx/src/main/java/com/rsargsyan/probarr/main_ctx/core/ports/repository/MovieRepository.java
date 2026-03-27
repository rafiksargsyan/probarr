package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
  Optional<Movie> findByTmdbId(Long tmdbId);

  @Query("SELECT m.id FROM Movie m WHERE m.forceScan = true OR m.lastScanAt IS NULL OR m.lastScanAt < :threshold")
  List<Long> findIdsDueForScan(@Param("threshold") Instant threshold);
}
