package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class MovieScannerService {

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
//    Instant threshold = Instant.now().minus(config.movieScanIntervalHours, ChronoUnit.HOURS);
    Instant threshold = Instant.now().minus(10, ChronoUnit.SECONDS);
    List<Long> ids = movieRepository.findIdsDueForScan(threshold);
    log.info("Found {} movie(s) due for scan", ids.size());
    for (Long id : ids) {
      try {
        movieScanTransactionService.scanMovie(id);
      } catch (Exception e) {
        log.error("Scan failed for movie id {}: {}", id, e.getMessage());
      }
    }
  }
}
