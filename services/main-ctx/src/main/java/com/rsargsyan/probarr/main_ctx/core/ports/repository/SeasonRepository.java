package com.rsargsyan.probarr.main_ctx.core.ports.repository;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {
  List<Season> findByTvShowId(Long tvShowId);
  Optional<Season> findByTvShowIdAndSeasonNumber(Long tvShowId, Integer seasonNumber);
}
