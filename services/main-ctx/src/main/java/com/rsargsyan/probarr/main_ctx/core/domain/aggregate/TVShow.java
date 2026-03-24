package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.exception.InvalidTitleException;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "tvshow")
public class TVShow extends AggregateRoot {

  @Getter
  @Column(nullable = false)
  private String originalTitle;

  @Getter
  @Column(unique = true)
  private String imdbId;

  @Getter
  @Column(unique = true)
  private Long tvdbId;

  @Getter
  @Column(unique = true)
  private Long sonarrId;

  @SuppressWarnings("unused")
  TVShow() {}

  public TVShow(String originalTitle, String imdbId, Long tvdbId, Long sonarrId) {
    if (originalTitle == null || originalTitle.isBlank()) {
      throw new InvalidTitleException();
    }
    this.originalTitle = originalTitle;
    this.imdbId = imdbId;
    this.tvdbId = tvdbId;
    this.sonarrId = sonarrId;
  }

  public void update(String originalTitle, String imdbId, Long tvdbId, Long sonarrId) {
    if (originalTitle == null || originalTitle.isBlank()) {
      throw new InvalidTitleException();
    }
    this.originalTitle = originalTitle;
    this.imdbId = imdbId;
    this.tvdbId = tvdbId;
    this.sonarrId = sonarrId;
    touch();
  }

  public void setSonarrId(Long sonarrId) {
    this.sonarrId = sonarrId;
    touch();
  }

  public void setTvdbId(Long tvdbId) {
    this.tvdbId = tvdbId;
    touch();
  }
}
