package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {
  Optional<Episode> findByTvShowIdAndSeasonNumberAndEpisodeNumber(Long tvShowId, Integer seasonNumber, Integer episodeNumber);
  Optional<Episode> findByTvShowIdAndAbsoluteNumber(Long tvShowId, Integer absoluteNumber);
  List<Episode> findByTvShowIdAndSeasonNumber(Long tvShowId, Integer seasonNumber);
}
