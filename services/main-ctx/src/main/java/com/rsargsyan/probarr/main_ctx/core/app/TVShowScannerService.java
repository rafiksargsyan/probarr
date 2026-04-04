package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.Config;
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
  private final TVShowScanTransactionService tvShowScanTransactionService;
  private final Config config;

  @Autowired
  public TVShowScannerService(TVShowRepository tvShowRepository,
                               TVShowScanTransactionService tvShowScanTransactionService,
                               Config config) {
    this.tvShowRepository = tvShowRepository;
    this.tvShowScanTransactionService = tvShowScanTransactionService;
    this.config = config;
  }

  public void scanDueShows() {
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
}
