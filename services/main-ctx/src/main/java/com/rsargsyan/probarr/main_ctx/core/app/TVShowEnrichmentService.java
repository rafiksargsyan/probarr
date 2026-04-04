package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.TVShowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class TVShowEnrichmentService {

  private final TVShowRepository tvShowRepository;
  private final TVShowEnrichmentTransactionService tvShowEnrichmentTransactionService;
  private final Config config;

  @Autowired
  public TVShowEnrichmentService(TVShowRepository tvShowRepository,
                                 TVShowEnrichmentTransactionService tvShowEnrichmentTransactionService,
                                 Config config) {
    this.tvShowRepository = tvShowRepository;
    this.tvShowEnrichmentTransactionService = tvShowEnrichmentTransactionService;
    this.config = config;
  }

  public void enrichDueTvShows() {
    Instant threshold = Instant.now().minus(config.tvShowEnrichmentIntervalSeconds, ChronoUnit.SECONDS);
    List<Long> ids = tvShowRepository.findIdsDueForEnrichment(threshold);
    log.info("Found {} TV show(s) due for enrichment", ids.size());
    for (Long id : ids) {
      try {
        tvShowEnrichmentTransactionService.enrichTvShow(id);
      } catch (Exception e) {
        log.error("Enrichment failed for TV show id {}: {}", id, e.getMessage());
      }
    }
  }
}
