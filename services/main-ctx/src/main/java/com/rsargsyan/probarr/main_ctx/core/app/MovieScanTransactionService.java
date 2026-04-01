package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import com.rsargsyan.probarr.main_ctx.core.domain.service.ReleaseTitleFilter;
import com.rsargsyan.probarr.main_ctx.core.domain.service.TitleLanguageParser;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Edition;
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

  public void scanMovie(Long movieId) {
    // Load just enough to call the indexer — no transaction yet
    Movie movie = movieRepository.findById(movieId)
        .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + movieId));
    if (!movie.getReleaseCandidates().isEmpty()) {
      log.info("Skipping scan for '{}' — {} candidate(s) already pending",
          movie.getOriginalTitle(), movie.getReleaseCandidates().size());
      return;
    }

    log.info("Scanning movie '{}' imdbId={}", movie.getOriginalTitle(), movie.getImdbId());

    List<IndexerClient.IndexerRelease> releases = indexerClient.searchMovies(
        movie.getImdbId(), movie.getOriginalTitle());

    log.info("Got {} releases from indexer for '{}'", releases.size(), movie.getOriginalTitle());

    // Build candidates from indexer results — pure computation, no DB involvement
    List<ReleaseCandidate> candidates = buildCandidates(releases, movie.getBlackList().stream().map(e -> e.infoHash()).toList(), movie.getWhiteList());

    // Short transaction: load fresh, add candidates, save
    persistCandidates(movieId, candidates);
    log.info("Scan complete for '{}': added {} candidate(s)", movie.getOriginalTitle(), candidates.size());
  }

  private List<ReleaseCandidate> buildCandidates(List<IndexerClient.IndexerRelease> releases,
                                                  List<String> blackList, List<String> whiteList) {
    List<ReleaseCandidate> result = new java.util.ArrayList<>();
    for (IndexerClient.IndexerRelease r : releases) {
      try {
        if (r.infoHash() == null || r.infoHash().isBlank()) {
          log.debug("Skipping '{}': no infoHash", r.title());
          continue;
        }
        if (r.seeders() == null || r.seeders() <= 0) {
          log.debug("Skipping '{}': no seeders", r.title());
          continue;
        }
        if (blackList.contains(r.infoHash())) {
          log.debug("Skipping '{}': blacklisted ({})", r.title(), r.infoHash());
          continue;
        }
        if (whiteList.contains(r.infoHash())) {
          log.debug("Skipping '{}': already whitelisted ({})", r.title(), r.infoHash());
          continue;
        }

        RipType ripType = RipType.fromTitle(r.title());
        if (ripType == null) {
          log.debug("Skipping '{}': unrecognized rip type", r.title());
          continue;
        }

        Resolution resolution = Resolution.fromTitle(r.title());
        if (resolution == null) {
          if (ripType.isLowQuality()) {
            resolution = Resolution.SD;
          } else {
            log.debug("Skipping '{}': unrecognized resolution for ripType={}", r.title(), ripType);
            continue;
          }
        }

        String rejection = ReleaseTitleFilter.reject(r.title(), r.sizeInBytes());
        if (rejection != null) {
          log.debug("Skipping '{}': rejected by filter '{}'", r.title(), rejection);
          continue;
        }

        result.add(new ReleaseCandidate(
            r.infoHash(),
            r.downloadUrl(),
            r.infoUrl(),
            TorrentTracker.fromJackettName(r.tracker()).orElse(TorrentTracker.UNKNOWN),
            r.sizeInBytes(),
            r.seeders(),
            resolution,
            ripType,
            Edition.fromTitle(r.title()),
            r.publishDate(),
            TitleLanguageParser.parse(r.title())
        ));
      } catch (Exception e) {
        log.warn("Skipping release '{}': {}", r.title(), e.getMessage());
      }
    }
    return result;
  }

  @Transactional
  public void markScanning(Long movieId) {
    Movie movie = movieRepository.findById(movieId)
        .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + movieId));
    movie.markScanning();
    movieRepository.save(movie);
  }

  @Transactional
  public void markScanDone(Long movieId) {
    movieRepository.findById(movieId).ifPresent(movie -> {
      movie.markScanDone();
      movieRepository.save(movie);
    });
  }

  @Transactional
  public void persistCandidates(Long movieId, List<ReleaseCandidate> candidates) {
    Movie movie = movieRepository.findById(movieId)
        .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + movieId));
    candidates.forEach(movie::addReleaseCandidate);
    movie.onScanCompleted();
    movieRepository.save(movie);
  }
}
