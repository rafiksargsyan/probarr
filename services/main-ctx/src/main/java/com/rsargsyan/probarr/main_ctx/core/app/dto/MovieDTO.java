package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.ReleaseCandidate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MovieDTO(
    String id,
    String originalTitle,
    Locale originalLocale,
    LocalDate releaseDate,
    Integer runtimeMinutes,
    Long tmdbId,
    String imdbId,
    List<String> alternativeTitles,
    List<ReleaseCandidate> releaseCandidates,
    List<String> blackList,
    List<String> whiteList,
    Instant lastScanAt,
    boolean forceScan,
    Instant createdAt
) {
  public static MovieDTO from(Movie movie) {
    return new MovieDTO(
        movie.getStrId(),
        movie.getOriginalTitle(),
        movie.getOriginalLocale(),
        movie.getReleaseDate(),
        movie.getRuntimeMinutes(),
        movie.getTmdbId(),
        movie.getImdbId(),
        movie.getAlternativeTitles(),
        movie.getReleaseCandidates(),
        movie.getBlackList(),
        movie.getWhiteList(),
        movie.getLastScanAt(),
        movie.isForceScan(),
        movie.getCreatedAt()
    );
  }
}
