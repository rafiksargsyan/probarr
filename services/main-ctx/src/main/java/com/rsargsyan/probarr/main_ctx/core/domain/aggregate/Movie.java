package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.BlacklistEntry;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.BlacklistReason;
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
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> alternativeTitles = new ArrayList<>();

  @Getter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<BlacklistEntry> blackList = new ArrayList<>();

  @Getter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> whiteList = new ArrayList<>();

  @Getter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> coolDownList = new ArrayList<>();

  @Getter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<ReleaseCandidate> releaseCandidates = new ArrayList<>();

  @Getter
  private Instant lastScanAt;

  @Getter
  private Instant lastEnrichedAt;

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

  public void addToCoolDown(String infoHash) {
    if (!coolDownList.contains(infoHash)) {
      coolDownList.add(infoHash);
      touch();
    }
  }

  public void removeFromCoolDown(String infoHash) {
    if (coolDownList.remove(infoHash)) {
      touch();
    }
  }

  public void addToBlackList(String infoHash, BlacklistReason reason) {
    boolean exists = blackList.stream().anyMatch(e -> e.infoHash().equals(infoHash));
    if (!exists) {
      blackList.add(new BlacklistEntry(infoHash, reason));
      touch();
    }
  }

  public void removeFromBlackList(String infoHash) {
    if (blackList.removeIf(e -> e.infoHash().equals(infoHash))) {
      touch();
    }
  }

  public boolean isBlacklisted(String infoHash) {
    return blackList.stream().anyMatch(e -> e.infoHash().equals(infoHash));
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

  public boolean enrichFromTmdb(String titleEnUs, String titleRu, List<String> romanizedTitles,
                                LocalDate releaseDate, Integer runtimeMinutes, String imdbId) {
    boolean updated = false;

    if (releaseDate != null && !releaseDate.equals(this.releaseDate)) {
      this.releaseDate = releaseDate;
      updated = true;
    }
    if (runtimeMinutes != null && !runtimeMinutes.equals(this.runtimeMinutes)) {
      this.runtimeMinutes = runtimeMinutes;
      updated = true;
    }
    if (imdbId != null && !imdbId.isBlank() && !imdbId.equals(this.imdbId)) {
      this.imdbId = imdbId;
      updated = true;
    }
    if (this.alternativeTitles.isEmpty()) {
      List<String> titles = new java.util.ArrayList<>();
      if (titleEnUs != null) titles.add(titleEnUs);
      if (titleRu != null) titles.add(titleRu);
      titles.addAll(romanizedTitles);
      if (!titles.isEmpty()) {
        this.alternativeTitles = titles;
        updated = true;
      }
    }

    this.lastEnrichedAt = Instant.now();
    if (updated) touch();
    return updated;
  }

  public void clearReleaseCandidates() {
    this.releaseCandidates = new ArrayList<>();
    touch();
  }

  public void onScanCompleted() {
    this.lastScanAt = Instant.now();
    this.forceScan = false;
    touch();
  }
}
