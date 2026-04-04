package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class MovieScannerService {

  private static final Duration SCAN_STALE_TIMEOUT = Duration.ofMinutes(10);

  private final MovieRepository movieRepository;
  private final MovieScanTransactionService movieScanTransactionService;
  private final Config config;

  @Autowired
  public MovieScannerService(MovieRepository movieRepository,
                             MovieScanTransactionService movieScanTransactionService,
                             Config config) {
    this.movieRepository = movieRepository;
    this.movieScanTransactionService = movieScanTransactionService;
    this.config = config;
  }

  public void scanDueMovies() {
    Instant threshold = Instant.now().minus(config.movieScanIntervalSeconds, ChronoUnit.SECONDS);
    List<Long> ids = movieRepository.findIdsDueForScan(threshold);
    log.info("Found {} movie(s) due for scan", ids.size());
    for (Long id : ids) {
      Movie movie = movieRepository.findById(id).orElse(null);
      if (movie == null) continue;
      if (movie.isScanningStale(SCAN_STALE_TIMEOUT)) {
        log.warn("Movie {} has stale scanning state, resetting", id);
        movieScanTransactionService.markScanDone(id);
      } else if (movie.isScanning()) {
        log.info("Skipping scan for '{}' — already scanning", movie.getOriginalTitle());
        continue;
      }
      try {
        movieScanTransactionService.scanMovie(id);
      } catch (Exception e) {
        log.error("Scan failed for movie id {}: {}", id, e.getMessage());
      }
      if (config.movieScanDelaySeconds > 0) {
        try {
          Thread.sleep(Duration.ofSeconds(config.movieScanDelaySeconds));
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          log.warn("Movie scan interrupted during delay");
          return;
        }
      }
    }
  }
}
