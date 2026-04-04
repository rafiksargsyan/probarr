package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;
import com.rsargsyan.probarr.main_ctx.core.exception.InvalidTitleException;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tvshow")
public class TVShow extends AggregateRoot {

  @Getter
  @Column(nullable = false)
  private String originalTitle;

  @Getter
  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Locale originalLocale;

  @Getter
  @Column(unique = true)
  private Long tmdbId;

  @Getter
  @Column(unique = true)
  private String imdbId;

  @Getter
  @Column(unique = true)
  private Long tvdbId;

  @Getter
  private LocalDate releaseDate;

  @Getter
  private boolean useTvdb = false;

  @Getter
  private Instant lastEnrichedAt;

  /** Deduplicated list of all known names across languages, used for title-prefix matching. */
  @Getter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> names = new ArrayList<>();

  @SuppressWarnings("unused")
  TVShow() {}

  public TVShow(String originalTitle, Locale originalLocale, Long tmdbId, String imdbId,
                Long tvdbId, LocalDate releaseDate, boolean useTvdb) {
    if (originalTitle == null || originalTitle.isBlank()) {
      throw new InvalidTitleException();
    }
    this.originalTitle = originalTitle;
    this.originalLocale = originalLocale;
    this.tmdbId = tmdbId;
    this.imdbId = imdbId;
    this.tvdbId = tvdbId;
    this.releaseDate = releaseDate;
    this.useTvdb = useTvdb;
  }

  public void update(String originalTitle, Locale originalLocale, Long tmdbId, String imdbId,
                     Long tvdbId, LocalDate releaseDate) {
    if (originalTitle == null || originalTitle.isBlank()) {
      throw new InvalidTitleException();
    }
    this.originalTitle = originalTitle;
    this.originalLocale = originalLocale;
    this.tmdbId = tmdbId;
    this.imdbId = imdbId;
    this.tvdbId = tvdbId;
    this.releaseDate = releaseDate;
    touch();
  }

  public void setTvdbId(Long tvdbId) {
    this.tvdbId = tvdbId;
    touch();
  }

  public void setTmdbId(Long tmdbId) {
    this.tmdbId = tmdbId;
    touch();
  }

  public void setUseTvdb(boolean useTvdb) {
    this.useTvdb = useTvdb;
    touch();
  }

  /**
   * Adds a name to the deduplicated names list (case-insensitive, trimmed).
   * Returns true if the name was new and was added.
   */
  public boolean addName(String name) {
    if (name == null || name.isBlank()) return false;
    String normalized = name.trim().toLowerCase();
    if (names.contains(normalized)) return false;
    names.add(normalized);
    touch();
    return true;
  }

  public boolean enrichFromTmdb(LocalDate releaseDate, String imdbId, Long tvdbId) {
    boolean updated = false;
    if (releaseDate != null && !releaseDate.equals(this.releaseDate)) {
      this.releaseDate = releaseDate;
      updated = true;
    }
    if (imdbId != null && !imdbId.isBlank() && !imdbId.equals(this.imdbId)) {
      this.imdbId = imdbId;
      updated = true;
    }
    if (tvdbId != null && !tvdbId.equals(this.tvdbId)) {
      this.tvdbId = tvdbId;
      updated = true;
    }
    this.lastEnrichedAt = Instant.now();
    if (updated) touch();
    return updated;
  }
}
