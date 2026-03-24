package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;

import java.time.Instant;

public record MovieDTO(
    String id,
    String originalTitle,
    Integer year,
    String imdbId,
    Long tmdbId,
    Long radarrId,
    Instant createdAt
) {
  public static MovieDTO from(Movie movie) {
    return new MovieDTO(
        movie.getStrId(),
        movie.getOriginalTitle(),
        movie.getYear(),
        movie.getImdbId(),
        movie.getTmdbId(),
        movie.getRadarrId(),
        movie.getCreatedAt()
    );
  }
}
