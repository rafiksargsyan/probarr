package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.exception.InvalidTitleException;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
public class Movie extends AggregateRoot {

  @Getter
  @Column(nullable = false)
  private String originalTitle;

  @Getter
  private Integer year;

  @Getter
  @Column(unique = true)
  private String imdbId;

  @Getter
  @Column(unique = true)
  private Long tmdbId;

  @Getter
  @Column(unique = true)
  private Long radarrId;

  @SuppressWarnings("unused")
  Movie() {}

  public Movie(String originalTitle, Integer year, String imdbId, Long tmdbId, Long radarrId) {
    if (originalTitle == null || originalTitle.isBlank()) {
      throw new InvalidTitleException();
    }
    this.originalTitle = originalTitle;
    this.year = year;
    this.imdbId = imdbId;
    this.tmdbId = tmdbId;
    this.radarrId = radarrId;
  }

  public void update(String originalTitle, Integer year, String imdbId, Long tmdbId, Long radarrId) {
    if (originalTitle == null || originalTitle.isBlank()) {
      throw new InvalidTitleException();
    }
    this.originalTitle = originalTitle;
    this.year = year;
    this.imdbId = imdbId;
    this.tmdbId = tmdbId;
    this.radarrId = radarrId;
    touch();
  }

  public void setRadarrId(Long radarrId) {
    this.radarrId = radarrId;
    touch();
  }

  public void setTmdbId(Long tmdbId) {
    this.tmdbId = tmdbId;
    touch();
  }
}
