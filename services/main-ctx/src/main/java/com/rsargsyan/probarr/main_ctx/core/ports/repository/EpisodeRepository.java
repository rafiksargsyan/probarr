package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {
  Optional<Episode> findByTvShowIdAndSeasonNumberAndEpisodeNumber(Long tvShowId, Integer seasonNumber, Integer episodeNumber);
  Optional<Episode> findByTvShowIdAndAbsoluteNumber(Long tvShowId, Integer absoluteNumber);
  List<Episode> findByTvShowId(Long tvShowId);
  List<Episode> findByTvShowIdAndSeasonNumber(Long tvShowId, Integer seasonNumber);

  @Query(value = "SELECT id FROM episode WHERE jsonb_array_length(release_candidates) > 0", nativeQuery = true)
  List<Long> findIdsWithPendingCandidates();
}
