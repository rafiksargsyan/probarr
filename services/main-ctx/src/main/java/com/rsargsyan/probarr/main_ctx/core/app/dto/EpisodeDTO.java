package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Episode;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.BlacklistEntry;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Release;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.ReleaseCandidate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EpisodeDTO(
    String id,
    String tvShowId,
    Integer seasonNumber,
    Integer episodeNumber,
    Integer absoluteNumber,
    LocalDate airDate,
    Integer runtimeSeconds,
    List<ReleaseCandidate> releaseCandidates,
    List<Release> releases,
    List<BlacklistEntry> blackList,
    List<String> whiteList,
    List<String> coolDownList,
    Instant lastScanAt,
    boolean scanning,
    Instant scanStartedAt
) {
  public static EpisodeDTO from(Episode episode) {
    return new EpisodeDTO(
        episode.getStrId(),
        episode.getTvShow().getStrId(),
        episode.getSeasonNumber(),
        episode.getEpisodeNumber(),
        episode.getAbsoluteNumber(),
        episode.getAirDate(),
        episode.getRuntimeSeconds(),
        episode.getReleaseCandidates(),
        episode.getReleases(),
        episode.getBlackList(),
        episode.getWhiteList(),
        episode.getCoolDownList(),
        episode.getLastScanAt(),
        episode.isScanning(),
        episode.getScanStartedAt()
    );
  }
}
