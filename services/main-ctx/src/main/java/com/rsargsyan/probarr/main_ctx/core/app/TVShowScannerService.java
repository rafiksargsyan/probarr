package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Episode;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.EpisodeRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.TVShowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class TVShowScannerService {

  private final TVShowRepository tvShowRepository;
  private final EpisodeRepository episodeRepository;
  private final TVShowScanTransactionService tvShowScanTransactionService;
  private final Config config;

  @Autowired
  public TVShowScannerService(TVShowRepository tvShowRepository,
                               EpisodeRepository episodeRepository,
                               TVShowScanTransactionService tvShowScanTransactionService,
                               Config config) {
    this.tvShowRepository = tvShowRepository;
    this.episodeRepository = episodeRepository;
    this.tvShowScanTransactionService = tvShowScanTransactionService;
    this.config = config;
  }

  public void scanDueShows() {
    resetStaleEpisodeScans();

    List<Long> tvShowIds = tvShowRepository.findAll().stream()
        .map(ts -> ts.getId())
        .toList();

    log.info("Scanning {} TV show(s)", tvShowIds.size());

    for (Long tvShowId : tvShowIds) {
      try {
        tvShowScanTransactionService.scanAndPersist(tvShowId);
      } catch (Exception e) {
        log.error("Scan failed for tvShowId={}: {}", tvShowId, e.getMessage());
      }
      if (config.tvShowScanDelaySeconds > 0) {
        try {
          Thread.sleep(Duration.ofSeconds(config.tvShowScanDelaySeconds));
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          log.warn("TV show scan interrupted during delay");
          return;
        }
      }
    }
  }

  private void resetStaleEpisodeScans() {
    List<Episode> scanning = episodeRepository.findByScanningTrue();
    Duration staleTimeout = Duration.ofSeconds(config.episodeScanStaleTimeoutSeconds);
    for (Episode episode : scanning) {
      if (episode.isScanningStale(staleTimeout)) {
        log.warn("Episode {} has stale scanning state, resetting", episode.getId());
        tvShowScanTransactionService.markEpisodeScanDone(episode.getId());
      }
    }
  }
}
