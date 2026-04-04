package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Episode;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Season;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.TVShow;
import com.rsargsyan.probarr.main_ctx.core.domain.service.EpisodeNumberResolver;
import com.rsargsyan.probarr.main_ctx.core.domain.service.ReleaseTitleFilter;
import com.rsargsyan.probarr.main_ctx.core.domain.service.TitleLanguageParser;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.ReleaseCandidate;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Resolution;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.RipType;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.TorrentTracker;
import com.rsargsyan.probarr.main_ctx.core.ports.client.IndexerClient;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.EpisodeRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.SeasonRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.TVShowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TVShowScanTransactionService {

  private final TVShowRepository tvShowRepository;
  private final SeasonRepository seasonRepository;
  private final EpisodeRepository episodeRepository;
  private final IndexerClient indexerClient;

  @Autowired
  public TVShowScanTransactionService(TVShowRepository tvShowRepository,
                                      SeasonRepository seasonRepository,
                                      EpisodeRepository episodeRepository,
                                      IndexerClient indexerClient) {
    this.tvShowRepository = tvShowRepository;
    this.seasonRepository = seasonRepository;
    this.episodeRepository = episodeRepository;
    this.indexerClient = indexerClient;
  }

  @Transactional
  public void scanAndPersist(Long tvShowId) {
    TVShow tvShow = tvShowRepository.findById(tvShowId)
        .orElseThrow(() -> new IllegalArgumentException("TVShow not found: " + tvShowId));

    List<Season> seasons = seasonRepository.findByTvShowId(tvShowId);
    if (seasons.isEmpty()) {
      log.info("No seasons for tvShowId={}", tvShowId);
      return;
    }

    List<Episode> allEpisodes = episodeRepository.findByTvShowId(tvShowId);
    if (allEpisodes.isEmpty()) {
      log.info("No episodes for tvShowId={}", tvShowId);
      return;
    }

    // Group episodes by season number for fast lookup, skipping those already in progress or done
    Map<Integer, List<Episode>> episodesBySeason = allEpisodes.stream()
        .filter(e -> e.getSeasonNumber() != null)
        .filter(e -> e.getReleaseCandidates().isEmpty())
        .collect(Collectors.groupingBy(Episode::getSeasonNumber));

    // Max episode number per season for EpisodeNumberResolver (use all episodes, not just eligible)
    Map<Integer, Integer> maxEpBySeason = allEpisodes.stream()
        .filter(e -> e.getSeasonNumber() != null && e.getEpisodeNumber() != null)
        .collect(Collectors.groupingBy(
            Episode::getSeasonNumber,
            Collectors.collectingAndThen(
                Collectors.maxBy(Comparator.comparingInt(Episode::getEpisodeNumber)),
                opt -> opt.map(Episode::getEpisodeNumber).orElse(1)
            )
        ));

    log.info("Scanning '{}' ({} season(s), {} episode(s))",
        tvShow.getOriginalTitle(), seasons.size(), allEpisodes.size());

    List<IndexerClient.IndexerRelease> releases = indexerClient.searchTvShowSeason(
        tvShow.getOriginalTitle(), 0);
    log.info("Got {} releases from indexer for '{}'", releases.size(), tvShow.getOriginalTitle());

    List<String> showNames = tvShow.getNames();
    int added = 0;

    for (IndexerClient.IndexerRelease r : releases) {
      try {
        if (r.infoHash() == null || r.infoHash().isBlank()) continue;
        if (r.seeders() == null || r.seeders() <= 0) continue;

        RipType ripType = RipType.fromTitle(r.title());
        if (ripType == null) continue;

        Resolution resolution = Resolution.fromTitle(r.title());
        if (resolution == null) {
          if (ripType.isLowQuality()) resolution = Resolution.SD;
          else continue;
        }

        String rejection = ReleaseTitleFilter.reject(r.title(), r.sizeInBytes());
        if (rejection != null) {
          log.debug("Skipping '{}': rejected by filter '{}'", r.title(), rejection);
          continue;
        }
        if (tvShow.getReleaseDate() != null && r.publishDate() != null
            && r.publishDate().isBefore(tvShow.getReleaseDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC))) {
          log.debug("Skipping '{}': published {} before show release {}", r.title(), r.publishDate(), tvShow.getReleaseDate());
          continue;
        }

        ReleaseCandidate candidate = new ReleaseCandidate(
            r.infoHash(),
            r.downloadUrl(),
            r.infoUrl(),
            TorrentTracker.fromJackettName(r.tracker()).orElse(TorrentTracker.UNKNOWN),
            r.sizeInBytes(),
            r.seeders(),
            resolution,
            ripType,
            null,
            r.publishDate(),
            TitleLanguageParser.parse(r.title())
        );

        for (Season season : seasons) {
          int seasonNumber = season.getSeasonNumber();
          List<Episode> episodes = episodesBySeason.get(seasonNumber);
          if (episodes == null || episodes.isEmpty()) continue;

          int maxEpisodeNumber = maxEpBySeason.getOrDefault(seasonNumber, 1);
          List<Integer> episodeNumbers = EpisodeNumberResolver.resolve(
              r.title(), seasonNumber, showNames, maxEpisodeNumber);

          if (episodeNumbers == null) {
            for (Episode ep : episodes) {
              if (!ep.isBlacklisted(r.infoHash())) {
                ep.addReleaseCandidate(candidate);
                added++;
              }
            }
          } else if (!episodeNumbers.isEmpty()) {
            for (int epNum : episodeNumbers) {
              episodes.stream()
                  .filter(e -> epNum == e.getEpisodeNumber())
                  .findFirst()
                  .ifPresent(ep -> {
                    if (!ep.isBlacklisted(r.infoHash())) ep.addReleaseCandidate(candidate);
                  });
            }
            added++;
          }
        }
      } catch (Exception e) {
        log.warn("Skipping release '{}': {}", r.title(), e.getMessage());
      }
    }

    episodeRepository.saveAll(allEpisodes);
    log.info("Scan done for '{}': added {} candidate(s)", tvShow.getOriginalTitle(), added);
  }

  @Transactional
  public void markEpisodeScanning(Long episodeId) {
    episodeRepository.findById(episodeId).ifPresent(ep -> {
      ep.markScanning();
      episodeRepository.save(ep);
    });
  }

  @Transactional
  public void markEpisodeScanDone(Long episodeId) {
    episodeRepository.findById(episodeId).ifPresent(ep -> {
      ep.markScanDone();
      episodeRepository.save(ep);
    });
  }

  @Transactional
  public void scanEpisode(Long episodeId) {
    Episode episode = episodeRepository.findById(episodeId)
        .orElseThrow(() -> new IllegalArgumentException("Episode not found: " + episodeId));
    TVShow tvShow = episode.getTvShow();
    List<String> showNames = tvShow.getNames();

    Integer seasonNumber = episode.getSeasonNumber();
    int maxEp = 1;
    if (seasonNumber != null) {
      List<Episode> allInSeason = episodeRepository.findByTvShowIdAndSeasonNumber(tvShow.getId(), seasonNumber);
      maxEp = allInSeason.stream()
          .filter(e -> e.getEpisodeNumber() != null)
          .mapToInt(Episode::getEpisodeNumber)
          .max().orElse(1);
    }

    List<IndexerClient.IndexerRelease> releases = indexerClient.searchTvShowSeason(
        tvShow.getOriginalTitle(), 0);
    log.info("Got {} releases from indexer for episode [{}/S{}/E{}]",
        releases.size(), tvShow.getOriginalTitle(), seasonNumber, episode.getEpisodeNumber());

    int added = 0;
    for (IndexerClient.IndexerRelease r : releases) {
      try {
        if (r.infoHash() == null || r.infoHash().isBlank()) continue;
        if (r.seeders() == null || r.seeders() <= 0) continue;

        RipType ripType = RipType.fromTitle(r.title());
        if (ripType == null) continue;

        Resolution resolution = Resolution.fromTitle(r.title());
        if (resolution == null) {
          if (ripType.isLowQuality()) resolution = Resolution.SD;
          else continue;
        }

        String rejection = ReleaseTitleFilter.reject(r.title(), r.sizeInBytes());
        if (rejection != null) continue;

        if (tvShow.getReleaseDate() != null && r.publishDate() != null
            && r.publishDate().isBefore(tvShow.getReleaseDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC))) {
          continue;
        }

        ReleaseCandidate candidate = new ReleaseCandidate(
            r.infoHash(), r.downloadUrl(), r.infoUrl(),
            TorrentTracker.fromJackettName(r.tracker()).orElse(TorrentTracker.UNKNOWN),
            r.sizeInBytes(), r.seeders(), resolution, ripType, null,
            r.publishDate(), TitleLanguageParser.parse(r.title())
        );

        if (seasonNumber != null) {
          List<Integer> episodeNumbers = EpisodeNumberResolver.resolve(
              r.title(), seasonNumber, showNames, maxEp);
          if (episodeNumbers == null) {
            if (!episode.isBlacklisted(r.infoHash())) {
              episode.addReleaseCandidate(candidate);
              added++;
            }
          } else if (episode.getEpisodeNumber() != null && episodeNumbers.contains(episode.getEpisodeNumber())) {
            if (!episode.isBlacklisted(r.infoHash())) {
              episode.addReleaseCandidate(candidate);
              added++;
            }
          }
        }
      } catch (Exception e) {
        log.warn("Skipping release '{}': {}", r.title(), e.getMessage());
      }
    }

    episode.onScanCompleted();
    episodeRepository.save(episode);
    log.info("Scan done for episode [{}]: added {} candidate(s)", episodeId, added);
  }
}
