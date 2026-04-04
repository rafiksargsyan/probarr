package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.TVShow;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;

import java.time.Instant;
import java.time.LocalDate;

public record TVShowDTO(
    String id,
    String originalTitle,
    Locale originalLocale,
    Long tmdbId,
    String imdbId,
    Long tvdbId,
    LocalDate releaseDate,
    boolean useTvdb,
    Instant lastEnrichedAt,
    Instant createdAt
) {
  public static TVShowDTO from(TVShow tvShow) {
    return new TVShowDTO(
        tvShow.getStrId(),
        tvShow.getOriginalTitle(),
        tvShow.getOriginalLocale(),
        tvShow.getTmdbId(),
        tvShow.getImdbId(),
        tvShow.getTvdbId(),
        tvShow.getReleaseDate(),
        tvShow.isUseTvdb(),
        tvShow.getLastEnrichedAt(),
        tvShow.getCreatedAt()
    );
  }
}

