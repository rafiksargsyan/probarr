package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.TVShow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TVShowRepository extends JpaRepository<TVShow, Long> {
  Optional<TVShow> findByImdbId(String imdbId);
  Optional<TVShow> findByTvdbId(Long tvdbId);
  Optional<TVShow> findBySonarrId(Long sonarrId);
}
