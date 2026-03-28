package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.ports.repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MovieProcessorService {

  private final MovieRepository movieRepository;
  private final MovieProcessorTransactionService movieProcessorTransactionService;

  @Autowired
  public MovieProcessorService(MovieRepository movieRepository,
                                MovieProcessorTransactionService movieProcessorTransactionService) {
    this.movieRepository = movieRepository;
    this.movieProcessorTransactionService = movieProcessorTransactionService;
  }

  public void processMoviesWithPendingCandidates() {
    List<Long> ids = movieRepository.findIdsWithPendingCandidates();
    log.info("Found {} movie(s) with pending candidates", ids.size());
    for (Long id : ids) {
      try {
        movieProcessorTransactionService.processMovie(id);
      } catch (Exception e) {
        log.error("Failed to process movie id {}: {}", id, e.getMessage());
      }
    }
  }
}
