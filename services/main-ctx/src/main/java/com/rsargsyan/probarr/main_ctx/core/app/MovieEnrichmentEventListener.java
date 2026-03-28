package com.rsargsyan.probarr.main_ctx.core.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class MovieEnrichmentEventListener {

  private final MovieEnrichmentTransactionService movieEnrichmentTransactionService;

  @Autowired
  public MovieEnrichmentEventListener(MovieEnrichmentTransactionService movieEnrichmentTransactionService) {
    this.movieEnrichmentTransactionService = movieEnrichmentTransactionService;
  }

  @Async
  @TransactionalEventListener
  public void onTmdbIdAssigned(MovieTmdbIdAssignedEvent event) {
    log.info("Triggering TMDB enrichment for movie id={} after tmdbId assignment", event.movieId());
    try {
      movieEnrichmentTransactionService.enrichMovie(event.movieId());
    } catch (Exception e) {
      log.error("Failed to enrich movie id={} after tmdbId assignment", event.movieId(), e);
    }
  }
}
