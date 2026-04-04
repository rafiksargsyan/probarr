package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.BlacklistEntry;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.BlacklistReason;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Release;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.ReleaseCandidate;
import com.rsargsyan.probarr.main_ctx.core.exception.InvalidEpisodeException;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Episode extends AggregateRoot {

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tvshow_id", nullable = false)
  private TVShow tvShow;

  @Getter
  @Column(name = "season_number")
  private Integer seasonNumber; // null for absolute-numbered episodes

  @Getter
  @Column(name = "episode_number")
  private Integer episodeNumber; // meaningful only when seasonNumber is present

  @Getter
  @Column(name = "absolute_number")
  private Integer absoluteNumber; // null for season-grouped episodes

  @Getter
  private LocalDate airDate;

  @Getter
  private Integer runtimeSeconds;

  @Getter
  private boolean scanning = false;

  @Getter
  private Instant scanStartedAt;

  @Getter
  private Instant lastScanAt;

  @Getter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<ReleaseCandidate> releaseCandidates = new ArrayList<>();

  @Getter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<Release> releases = new ArrayList<>();

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

  @SuppressWarnings("unused")
  Episode() {}

  public Episode(TVShow tvShow, Integer seasonNumber, Integer episodeNumber,
                 Integer absoluteNumber, LocalDate airDate, Integer runtimeSeconds) {
    validate(seasonNumber, episodeNumber, absoluteNumber);
    this.tvShow = tvShow;
    this.seasonNumber = seasonNumber;
    this.episodeNumber = episodeNumber;
    this.absoluteNumber = absoluteNumber;
    this.airDate = airDate;
    this.runtimeSeconds = runtimeSeconds;
  }

  public void update(Integer seasonNumber, Integer episodeNumber, Integer absoluteNumber,
                     LocalDate airDate, Integer runtimeSeconds) {
    validate(seasonNumber, episodeNumber, absoluteNumber);
    this.seasonNumber = seasonNumber;
    this.episodeNumber = episodeNumber;
    this.absoluteNumber = absoluteNumber;
    this.airDate = airDate;
    this.runtimeSeconds = runtimeSeconds;
    touch();
  }

  private static void validate(Integer seasonNumber, Integer episodeNumber, Integer absoluteNumber) {
    boolean hasSeasonEpisode = seasonNumber != null && episodeNumber != null;
    boolean hasAbsolute = absoluteNumber != null;
    if (!hasSeasonEpisode && !hasAbsolute) {
      throw new InvalidEpisodeException(
          "Episode must have either (seasonNumber + episodeNumber) or absoluteNumber");
    }
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

  public boolean addRelease(Release newRelease) {
    List<Release> toReplace = new ArrayList<>();
    for (Release existing : releases) {
      Integer cmp = Release.compare(existing, newRelease);
      if (cmp == null) continue;
      if (cmp > 0) return false;
      if (cmp == 0) {
        if (existing.infoHash().equals(newRelease.infoHash())) return false;
        if (Release.compare2(existing, newRelease) >= 0) return false;
        releases.remove(existing);
        releases.add(newRelease);
        touch();
        return true;
      }
      toReplace.add(existing);
    }
    releases.removeAll(toReplace);
    releases.add(newRelease);
    touch();
    return true;
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

  public void markScanning() {
    this.scanning = true;
    this.scanStartedAt = Instant.now();
    touch();
  }

  public void markScanDone() {
    this.scanning = false;
    touch();
  }

  public boolean isScanningStale(Duration timeout) {
    return scanning && scanStartedAt != null && scanStartedAt.plus(timeout).isBefore(Instant.now());
  }

  public void onScanCompleted() {
    this.lastScanAt = Instant.now();
    this.scanning = false;
    touch();
  }

  public void clearReleaseCandidates() {
    this.releaseCandidates = new ArrayList<>();
    touch();
  }
}
