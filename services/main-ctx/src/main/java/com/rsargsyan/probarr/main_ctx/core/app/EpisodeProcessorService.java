package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.ports.repository.EpisodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EpisodeProcessorService {

  private final EpisodeRepository episodeRepository;
  private final EpisodeProcessorTransactionService episodeProcessorTransactionService;

  @Autowired
  public EpisodeProcessorService(EpisodeRepository episodeRepository,
                                  EpisodeProcessorTransactionService episodeProcessorTransactionService) {
    this.episodeRepository = episodeRepository;
    this.episodeProcessorTransactionService = episodeProcessorTransactionService;
  }

  public void processEpisodesWithPendingCandidates() {
    List<Long> ids = episodeRepository.findIdsWithPendingCandidates();
    log.info("Found {} episode(s) with pending release candidates", ids.size());
    for (Long id : ids) {
      try {
        episodeProcessorTransactionService.processEpisode(id);
      } catch (Exception e) {
        log.error("Processing failed for episode id {}: {}", id, e.getMessage());
      }
    }
  }
}
