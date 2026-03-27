package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.ReleaseCandidate;
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
public class Movie extends AggregateRoot {

  @Getter
  @Column(nullable = false)
  private String originalTitle;

  @Getter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Locale originalLocale;

  @Getter
  private LocalDate releaseDate;

  @Getter
  private Integer runtimeMinutes;

  @Getter
  @Column(unique = true)
  private Long tmdbId;

  @Getter
  @Column(unique = true)
  private String imdbId;

  @Getter
  @ElementCollection
  @CollectionTable(name = "movie_alternative_titles", joinColumns = @JoinColumn(name = "movie_id"))
  @Column(name = "title")
  private List<String> alternativeTitles = new ArrayList<>();

  @Getter
  @ElementCollection
  @CollectionTable(name = "movie_blacklist", joinColumns = @JoinColumn(name = "movie_id"))
  @Column(name = "candidate_id")
  private List<String> blackList = new ArrayList<>();

  @Getter
  @ElementCollection
  @CollectionTable(name = "movie_whitelist", joinColumns = @JoinColumn(name = "movie_id"))
  @Column(name = "candidate_id")
  private List<String> whiteList = new ArrayList<>();

  @Getter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<ReleaseCandidate> releaseCandidates = new ArrayList<>();

  @Getter
  private Instant lastScanAt;

  @Getter
  private boolean forceScan = false;

  @SuppressWarnings("unused")
  Movie() {}

  public Movie(String originalTitle, Locale originalLocale, LocalDate releaseDate,
               Integer runtimeMinutes, Long tmdbId, String imdbId) {
    if (originalTitle == null || originalTitle.isBlank()) {
      throw new InvalidTitleException();
    }
    this.originalTitle = originalTitle;
    this.originalLocale = originalLocale;
    this.releaseDate = releaseDate;
    this.runtimeMinutes = runtimeMinutes;
    this.tmdbId = tmdbId;
    this.imdbId = imdbId;
  }

  public void update(String originalTitle, Locale originalLocale, LocalDate releaseDate,
                     Integer runtimeMinutes, Long tmdbId, String imdbId) {
    if (originalTitle == null || originalTitle.isBlank()) {
      throw new InvalidTitleException();
    }
    this.originalTitle = originalTitle;
    this.originalLocale = originalLocale;
    this.releaseDate = releaseDate;
    this.runtimeMinutes = runtimeMinutes;
    this.tmdbId = tmdbId;
    this.imdbId = imdbId;
    touch();
  }

  public void setTmdbId(Long tmdbId) {
    this.tmdbId = tmdbId;
    touch();
  }

  public void setAlternativeTitles(List<String> titles) {
    this.alternativeTitles = new ArrayList<>(titles);
    touch();
  }

  public void addReleaseCandidate(ReleaseCandidate candidate) {
    boolean exists = releaseCandidates.stream()
        .anyMatch(rc -> rc.infoHash().equals(candidate.infoHash()));
    if (!exists) {
      releaseCandidates.add(candidate);
      touch();
    }
  }

  public void removeReleaseCandidate(String infoHash) {
    if (releaseCandidates.removeIf(rc -> rc.infoHash().equals(infoHash))) {
      touch();
    }
  }

  public void addToBlackList(String infoHash) {
    if (!blackList.contains(infoHash)) {
      blackList.add(infoHash);
      touch();
    }
  }

  public void removeFromBlackList(String infoHash) {
    if (blackList.remove(infoHash)) {
      touch();
    }
  }

  public void addToWhiteList(String infoHash) {
    if (!whiteList.contains(infoHash)) {
      whiteList.add(infoHash);
      touch();
    }
  }

  public void removeFromWhiteList(String infoHash) {
    if (whiteList.remove(infoHash)) {
      touch();
    }
  }

  public void setForceScan(boolean forceScan) {
    this.forceScan = forceScan;
    touch();
  }

  public void onScanCompleted() {
    this.lastScanAt = Instant.now();
    this.forceScan = false;
    touch();
  }
}
