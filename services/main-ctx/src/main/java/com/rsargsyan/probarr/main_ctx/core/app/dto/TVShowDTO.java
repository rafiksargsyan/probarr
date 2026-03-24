package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.TVShow;

import java.time.Instant;

public record TVShowDTO(
    String id,
    String originalTitle,
    String imdbId,
    Long tvdbId,
    Long sonarrId,
    Instant createdAt
) {
  public static TVShowDTO from(TVShow tvShow) {
    return new TVShowDTO(
        tvShow.getStrId(),
        tvShow.getOriginalTitle(),
        tvShow.getImdbId(),
        tvShow.getTvdbId(),
        tvShow.getSonarrId(),
        tvShow.getCreatedAt()
    );
  }
}
