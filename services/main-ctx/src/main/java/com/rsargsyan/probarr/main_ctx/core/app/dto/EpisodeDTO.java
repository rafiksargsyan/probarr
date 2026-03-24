package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Episode;

import java.time.LocalDate;

public record EpisodeDTO(
    String id,
    String tvShowId,
    Integer seasonNumber,
    Integer episodeNumber,
    Integer absoluteNumber,
    LocalDate airDate,
    Integer runtime
) {
  public static EpisodeDTO from(Episode episode) {
    return new EpisodeDTO(
        episode.getStrId(),
        episode.getTvShow().getStrId(),
        episode.getSeasonNumber(),
        episode.getEpisodeNumber(),
        episode.getAbsoluteNumber(),
        episode.getAirDate(),
        episode.getRuntime()
    );
  }
}
