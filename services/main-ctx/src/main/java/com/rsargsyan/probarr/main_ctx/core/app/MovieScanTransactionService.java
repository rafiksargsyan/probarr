package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.Config;
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MovieScanTransactionService {

  private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");

  private final MovieRepository movieRepository;
  private final IndexerClient indexerClient;
  private final Config config;

  @Autowired
  public MovieScanTransactionService(MovieRepository movieRepository, IndexerClient indexerClient, Config config) {
    this.movieRepository = movieRepository;
    this.indexerClient = indexerClient;
    this.config = config;
  }

  public void scanMovie(Long movieId) {
    Movie movie = movieRepository.findById(movieId)
        .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + movieId));
    if (!movie.getReleaseCandidates().isEmpty()) {
      log.info("Skipping scan for '{}' — {} candidate(s) already pending",
          movie.getOriginalTitle(), movie.getReleaseCandidates().size());
      return;
    }

    log.info("Scanning movie '{}'", movie.getOriginalTitle());
    List<IndexerClient.IndexerRelease> releases = indexerClient.searchMovies(movie.getOriginalTitle());
    log.info("Got {} releases from indexer for '{}'", releases.size(), movie.getOriginalTitle());

    List<ReleaseCandidate> candidates = buildCandidates(releases, movie);

    persistCandidates(movieId, candidates);
    log.info("Scan complete for '{}': added {} candidate(s)", movie.getOriginalTitle(), candidates.size());
  }

  private List<ReleaseCandidate> buildCandidates(List<IndexerClient.IndexerRelease> releases, Movie movie) {
    List<String> blackList = movie.getBlackList().stream().map(e -> e.infoHash()).toList();
    List<String> whiteList = movie.getWhiteList();
    List<ReleaseCandidate> result = new ArrayList<>();
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
          log.debug("Skipping '{}': blacklisted", r.title());
          continue;
        }
        if (whiteList.contains(r.infoHash())) {
          log.debug("Skipping '{}': already whitelisted", r.title());
          continue;
        }
        RipType ripType = RipType.fromTitle(r.title());
        if (ripType == null) {
          log.debug("Skipping '{}': unrecognized rip type", r.title());
          continue;
        }
        Resolution resolution = Resolution.fromTitle(r.title());
        if (resolution == null) {
          if (ripType.isLowQuality()) resolution = Resolution.SD;
          else {
            log.debug("Skipping '{}': unrecognized resolution", r.title());
            continue;
          }
        }
        String rejection = ReleaseTitleFilter.reject(r.title(), r.sizeInBytes());
        if (rejection != null) {
          log.debug("Skipping '{}': rejected by filter '{}'", r.title(), rejection);
          continue;
        }
        if (movie.getReleaseDate() != null && r.publishDate() != null
            && r.publishDate().isBefore(movie.getReleaseDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC))) {
          log.debug("Skipping '{}': published {} before movie release {}", r.title(), r.publishDate(), movie.getReleaseDate());
          continue;
        }
        if (movie.getReleaseDate() != null) {
          int releaseYear = movie.getReleaseDate().getYear();
          Matcher m = YEAR_PATTERN.matcher(r.title());
          List<Integer> yearsInTitle = new ArrayList<>();
          while (m.find()) yearsInTitle.add(Integer.parseInt(m.group(1)));
          if (!yearsInTitle.isEmpty() && yearsInTitle.stream().noneMatch(y -> y == releaseYear)) {
            log.debug("Skipping '{}': title years {} don't match release year {}", r.title(), yearsInTitle, releaseYear);
            continue;
          }
        }
        if (movie.getRuntimeMinutes() != null && r.sizeInBytes() != null && r.sizeInBytes() > 0) {
          long bitrateKbps = (r.sizeInBytes() * 8L) / (movie.getRuntimeMinutes() * 60L * 1000L);
          if (bitrateKbps < config.minBitrateKbps) {
            log.debug("Skipping '{}': bitrate {}kbps below minimum {}kbps", r.title(), bitrateKbps, config.minBitrateKbps);
            continue;
          }
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
