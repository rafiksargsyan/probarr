package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.ReleaseCandidate;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Resolution;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.RipType;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.TorrentTracker;
import com.rsargsyan.probarr.main_ctx.core.ports.client.IndexerClient;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class MovieScanTransactionService {

  private final MovieRepository movieRepository;
  private final IndexerClient indexerClient;

  @Autowired
  public MovieScanTransactionService(MovieRepository movieRepository, IndexerClient indexerClient) {
    this.movieRepository = movieRepository;
    this.indexerClient = indexerClient;
  }

  @Transactional
  public void scanMovie(Long movieId) {
    Movie movie = movieRepository.findById(movieId)
        .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + movieId));
    log.info("Scanning movie '{}' imdbId={}", movie.getOriginalTitle(), movie.getImdbId());

    List<IndexerClient.IndexerRelease> releases = indexerClient.searchMovies(
        movie.getImdbId(), movie.getOriginalTitle());

    log.info("Got {} releases from indexer for '{}'", releases.size(), movie.getOriginalTitle());

    int added = 0;
    for (IndexerClient.IndexerRelease r : releases) {
      try {
        if (r.infoHash() == null || r.infoHash().isBlank()) continue;
        if (r.seeders() == null || r.seeders() <= 0) continue;
        if (movie.getBlackList().contains(r.infoHash())) continue;
        if (movie.getWhiteList().contains(r.infoHash())) continue;

        ReleaseCandidate candidate = new ReleaseCandidate(
            r.infoHash(),
            r.downloadUrl(),
            r.infoUrl(),
            TorrentTracker.fromJackettName(r.tracker()).orElse(TorrentTracker.UNKNOWN),
            r.sizeInBytes(),
            r.seeders(),
            Resolution.fromTitle(r.title()),
            RipType.fromTitle(r.title()),
            r.publishDate(),
            List.of(),
            false
        );

        movie.addReleaseCandidate(candidate);
        added++;
      } catch (Exception e) {
        log.warn("Skipping release '{}': {}", r.title(), e.getMessage());
      }
    }

    movie.onScanCompleted();
    movieRepository.save(movie);
    log.info("Scan complete for '{}': added {} candidate(s)", movie.getOriginalTitle(), added);
  }
}
