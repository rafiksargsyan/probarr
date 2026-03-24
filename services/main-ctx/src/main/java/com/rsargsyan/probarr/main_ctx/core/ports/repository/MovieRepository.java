package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
  Optional<Movie> findByImdbId(String imdbId);
  Optional<Movie> findByTmdbId(Long tmdbId);
  Optional<Movie> findByRadarrId(Long radarrId);
}
