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
public class MovieEnrichmentService {

  private final MovieRepository movieRepository;
  private final MovieEnrichmentTransactionService transactionService;
  private final Config config;

  @Autowired
  public MovieEnrichmentService(MovieRepository movieRepository,
                                MovieEnrichmentTransactionService transactionService,
                                Config config) {
    this.movieRepository = movieRepository;
    this.transactionService = transactionService;
    this.config = config;
  }

  public void enrichDueMovies() {
    Instant threshold = Instant.now().minus(config.movieEnrichmentIntervalSeconds, ChronoUnit.SECONDS);
    List<Long> ids = movieRepository.findIdsDueForEnrichment(threshold);
    log.info("Found {} movie(s) due for TMDB enrichment", ids.size());
    for (Long id : ids) {
      try {
        transactionService.enrichMovie(id);
      } catch (Exception e) {
        log.error("Failed to enrich movie id={}: {}", id, e.getMessage());
      }
    }
  }
}
