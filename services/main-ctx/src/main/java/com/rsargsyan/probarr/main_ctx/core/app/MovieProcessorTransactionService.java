package com.rsargsyan.probarr.main_ctx.core.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Release;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.BlacklistReason;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.ReleaseCandidate;
import com.rsargsyan.probarr.main_ctx.core.ports.client.GrabberrClient;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.MovieRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.ReleaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class MovieProcessorTransactionService {

  private final MovieRepository movieRepository;
  private final ReleaseRepository releaseRepository;
  private final GrabberrClient grabberrClient;
  private final ObjectMapper objectMapper;
  private final String prowlarrApiKey;
  private final HttpClient httpClient;
  private final Config config;

  @Autowired
  public MovieProcessorTransactionService(MovieRepository movieRepository,
                                          ReleaseRepository releaseRepository,
                                          GrabberrClient grabberrClient,
                                          ObjectMapper objectMapper,
                                          Config config) {
    this.movieRepository = movieRepository;
    this.releaseRepository = releaseRepository;
    this.grabberrClient = grabberrClient;
    this.objectMapper = objectMapper;
    this.prowlarrApiKey = config.prowlarrApiKey;
    this.config = config;
    this.httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  @Transactional
  public void processMovie(Long movieId) {
    Movie movie = movieRepository.findById(movieId)
        .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + movieId));

    if (movie.getReleaseCandidates().isEmpty()) return;

    List<ReleaseCandidate> candidates = movie.getReleaseCandidates().stream()
        .sorted(releaseCandidateComparator())
        .toList();

    boolean shouldBreak = false;
    for (ReleaseCandidate rc : candidates) {
      if (movie.isBlacklisted(rc.infoHash())) continue;
      if (movie.getWhiteList().contains(rc.infoHash())) continue;
      if (movie.getCoolDownList().contains(rc.infoHash())) continue;

      try {
        Optional<GrabberrClient.TorrentDownloadDTO> torrentOpt = grabberrClient.findByInfoHash(rc.infoHash());

        if (torrentOpt.isEmpty()) {
          log.info("Submitting torrent for rc={} movie='{}'", rc.infoHash(), movie.getOriginalTitle());
          try {
            if (rc.downloadUrl().startsWith("magnet:")) {
              grabberrClient.submitTorrent(rc.downloadUrl());
            } else {
              String magnetOrNull = resolveRedirectToMagnet(rc.downloadUrl());
              if (magnetOrNull != null) {
                grabberrClient.submitTorrent(magnetOrNull);
              } else {
                byte[] torrentBytes = fetchTorrentBytes(rc.downloadUrl());
                grabberrClient.submitTorrentFile(torrentBytes);
              }
            }
          } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("Permanent error submitting torrent rc={}: {}, adding to cool-down", rc.infoHash(), e.getMessage());
            movie.addToCoolDown(rc.infoHash());
          } catch (Exception e) {
            log.warn("Failed to submit torrent rc={}: {}, will retry next cycle", rc.infoHash(), e.getMessage());
          }
          shouldBreak = true;
        } else {
          GrabberrClient.TorrentDownloadDTO torrent = torrentOpt.get();

          if (torrent.status() == GrabberrClient.TorrentStatus.QUEUED
              || torrent.status() == GrabberrClient.TorrentStatus.FETCHING_METADATA) {
            int timeoutSeconds = torrent.status() == GrabberrClient.TorrentStatus.QUEUED
                ? config.queuedTimeoutSeconds
                : config.metadataFetchTimeoutSeconds;
            Instant deadline = torrent.createdAt() != null
                ? torrent.createdAt().plusSeconds(timeoutSeconds)
                : null;
            if (deadline != null && Instant.now().isAfter(deadline)) {
              log.warn("Torrent rc={} stuck in {} past timeout, moving to cool-down",
                  rc.infoHash(), torrent.status());
              deleteTorrentDownloadQuietly(torrent.id(), rc.infoHash());
              movie.addToCoolDown(rc.infoHash());
            } else {
              log.debug("Torrent rc={} status={}", rc.infoHash(), torrent.status());
              shouldBreak = true;
            }
          } else if (torrent.status() == GrabberrClient.TorrentStatus.FAILED) {
            log.warn("Torrent rc={} failed metadata fetch, blacklisting", rc.infoHash());
            movie.addToBlackList(rc.infoHash(), BlacklistReason.TORRENT_FAILED);
            deleteTorrentDownloadQuietly(torrent.id(), rc.infoHash());
          } else { // READY
            GrabberrClient.TorrentFile mediaFile = findMediaFile(torrent.files(), movie);
            if (mediaFile == null) {
              log.warn("No media file found for rc={}, blacklisting", rc.infoHash());
              movie.addToBlackList(rc.infoHash(), BlacklistReason.NO_MEDIA_FILE);
              deleteTorrentDownloadQuietly(torrent.id(), rc.infoHash());
            } else if (mediaFile.sizeBytes() > config.maxFileSizeBytes) {
              log.warn("Media file too large ({} bytes > {} bytes) for rc={}, blacklisting",
                  mediaFile.sizeBytes(), config.maxFileSizeBytes, rc.infoHash());
              movie.addToBlackList(rc.infoHash(), BlacklistReason.FILE_TOO_LARGE);
              deleteTorrentDownloadQuietly(torrent.id(), rc.infoHash());
            } else {
              GrabberrClient.FileDownloadDTO fileStatus = grabberrClient.getFileStatus(torrent.id(), mediaFile.index());
              if (fileStatus == null) {
                log.info("Claiming file index={} for rc={}", mediaFile.index(), rc.infoHash());
                grabberrClient.claimFile(torrent.id(), mediaFile.index());
                shouldBreak = true;
              } else if (fileStatus.status() == GrabberrClient.FileDownloadStatus.DOWNLOADED) {
                if (fileStatus.metadata() != null) {
                  if (validateMetadata(fileStatus.metadata(), movie, rc)) {
                    log.info("Metadata valid for rc={}, requesting S3 cache", rc.infoHash());
                    grabberrClient.cacheFile(torrent.id(), mediaFile.index());
                  } else {
                    movie.addToBlackList(rc.infoHash(), BlacklistReason.PROCESSING_FAILED);
                    unclaimFileQuietly(torrent.id(), mediaFile.index(), rc.infoHash());
                  }
                } else {
                  log.debug("File rc={} DOWNLOADED but no metadata yet, requesting S3 cache", rc.infoHash());
                  grabberrClient.cacheFile(torrent.id(), mediaFile.index());
                }
                shouldBreak = true;
              } else if (fileStatus.status() == GrabberrClient.FileDownloadStatus.DONE) {
                if (processMediaFile(movie, rc, mediaFile, fileStatus)) {
                  movie.addToWhiteList(rc.infoHash());
                } else {
                  movie.addToBlackList(rc.infoHash(), BlacklistReason.PROCESSING_FAILED);
                }
                shouldBreak = true;
              } else if (fileStatus.status() == GrabberrClient.FileDownloadStatus.FAILED) {
                log.warn("File download failed for rc={}, blacklisting", rc.infoHash());
                movie.addToBlackList(rc.infoHash(), BlacklistReason.FILE_DOWNLOAD_FAILED);
                unclaimFileQuietly(torrent.id(), mediaFile.index(), rc.infoHash());
              } else {
                boolean timedOut = isFileDownloadTimedOut(fileStatus);
                if (timedOut) {
                  log.warn("File download timed out for rc={} status={}, unclaiming and moving to cool-down",
                      rc.infoHash(), fileStatus.status());
                  unclaimFileQuietly(torrent.id(), mediaFile.index(), rc.infoHash());
                  movie.addToCoolDown(rc.infoHash());
                } else {
                  log.debug("File rc={} status={} progress={}", rc.infoHash(), fileStatus.status(), fileStatus.progress());
                  shouldBreak = true;
                }
              }
            }
          }
        }
      } catch (ResourceAccessException e) {
        log.warn("Grabberr unreachable while processing rc={} for movie '{}', will retry next cycle: {}",
            rc.infoHash(), movie.getOriginalTitle(), e.getMessage());
        shouldBreak = true;
      } catch (Exception e) {
        log.error("Error processing rc={} for movie '{}': {}", rc.infoHash(), movie.getOriginalTitle(), e.getMessage());
        movie.addToBlackList(rc.infoHash(), BlacklistReason.PROCESSING_ERROR);
      }

      if (shouldBreak) break;
    }

    // Check if all candidates are resolved
    boolean allDone = candidates.stream().allMatch(rc ->
        movie.isBlacklisted(rc.infoHash())
        || movie.getWhiteList().contains(rc.infoHash())
        || movie.getCoolDownList().contains(rc.infoHash()));
    if (allDone) {
      log.info("All candidates processed for '{}', clearing RC list", movie.getOriginalTitle());
      movie.clearReleaseCandidates();
    }

    movieRepository.save(movie);
  }

  private GrabberrClient.TorrentFile findMediaFile(List<GrabberrClient.TorrentFile> files, Movie movie) {
    List<GrabberrClient.TorrentFile> candidates = files.stream()
        .filter(f -> {
          String name = f.name().toLowerCase();
          return (name.endsWith(".mkv") || name.endsWith(".mp4") || name.endsWith(".avi"))
              && !name.contains("sample");
        })
        .toList();

    if (candidates.isEmpty()) return null;

    record Scored(GrabberrClient.TorrentFile file, long score) {}
    List<Scored> scored = candidates.stream()
        .map(f -> new Scored(f, scoreFileName(f.name(), movie)))
        .sorted(Comparator.comparingLong(Scored::score).reversed())
        .toList();

    if (candidates.size() == 1) {
      return scored.get(0).score() > 0 ? scored.get(0).file() : null;
    }

    if (scored.get(0).score() > 0 && scored.get(0).score() > scored.get(1).score()) {
      return scored.get(0).file();
    }
    return null;
  }

  private boolean isFileDownloadTimedOut(GrabberrClient.FileDownloadDTO fileStatus) {
    if (fileStatus.status() == GrabberrClient.FileDownloadStatus.SUBMITTED) {
      return fileStatus.createdAt() != null
          && Instant.now().isAfter(fileStatus.createdAt().plusSeconds(config.fileSubmittedTimeoutSeconds));
    }
    // DOWNLOADING or TRANSFERRING — use downloadingAt, fall back to createdAt for pre-existing records
    Instant baseline = fileStatus.downloadingAt() != null
        ? fileStatus.downloadingAt()
        : fileStatus.createdAt();
    if (baseline == null) return false;

    long elapsedSeconds = Instant.now().getEpochSecond() - baseline.getEpochSecond();

    // Hard timeout
    if (elapsedSeconds > config.fileDownloadingTimeoutSeconds) return true;

    // After minimum observation period, extrapolate from actual progress
    if (elapsedSeconds >= config.fileProgressObservationSeconds
        && fileStatus.progress() != null && fileStatus.progress() > 0) {
      long impliedTotalSeconds = (long) (elapsedSeconds / fileStatus.progress());
      if (impliedTotalSeconds > config.fileDownloadingTimeoutSeconds) {
        log.warn("File download progress too slow: {}% in {}s, implied total {}s exceeds timeout {}s",
            String.format("%.2f", fileStatus.progress() * 100), elapsedSeconds,
            impliedTotalSeconds, config.fileDownloadingTimeoutSeconds);
        return true;
      }
    }

    return false;
  }

  private long scoreFileName(String fileName, Movie movie) {
    String name = fileName.toLowerCase();

    long yearBonus = 0;
    if (movie.getReleaseDate() != null) {
      String year = String.valueOf(movie.getReleaseDate().getYear());
      int idx = 0;
      while ((idx = name.indexOf(year, idx)) >= 0) { yearBonus++; idx++; }
    }

    List<String> titles = new ArrayList<>(movie.getAlternativeTitles());
    titles.add(movie.getOriginalTitle());

    long bestTitleScore = titles.stream().mapToLong(title -> {
      String[] tokens = title.toLowerCase().split("[\\s:,'.()+\\-]+");
      return Arrays.stream(tokens)
          .filter(t -> t.length() >= 3 && name.contains(t))
          .count();
    }).max().orElse(0L);

    return bestTitleScore + yearBonus;
  }

  private boolean processMediaFile(Movie movie, ReleaseCandidate rc,
                                   GrabberrClient.TorrentFile mediaFile,
                                   GrabberrClient.FileDownloadDTO fileStatus) {
    try {
      String signedUrl = fileStatus.signedUrl();
      ProcessBuilder pb = new ProcessBuilder(
          "ffprobe", "-v", "quiet", "-print_format", "json", "-show_format", "-show_streams", signedUrl);
      pb.redirectErrorStream(true);
      Process process = pb.start();
      String output = new String(process.getInputStream().readAllBytes());
      int exitCode = process.waitFor();

      if (exitCode != 0) {
        log.warn("ffprobe failed (exit={}) for rc={}", exitCode, rc.infoHash());
        return false;
      }

      JsonNode root = objectMapper.readTree(output);
      JsonNode streams = root.path("streams");
      JsonNode format = root.path("format");

      if (streams.isEmpty() || !"video".equals(streams.get(0).path("codec_type").asText())) {
        log.warn("First stream is not video for rc={}", rc.infoHash());
        return false;
      }

      String videoCodec = streams.get(0).path("codec_name").asText("unknown");

      boolean hasAudio = false;
      for (JsonNode s : streams) {
        if ("audio".equals(s.path("codec_type").asText())) { hasAudio = true; break; }
      }
      if (!hasAudio) {
        log.warn("No audio stream for rc={}", rc.infoHash());
        return false;
      }

      int runtimeSeconds = 0;
      String durationStr = format.path("duration").asText(null);
      if (durationStr != null) {
        double duration = Double.parseDouble(durationStr);
        if (movie.getRuntimeMinutes() != null) {
          double expected = movie.getRuntimeMinutes() * 60.0;
          if (Math.abs(duration - expected) > 0.2 * expected) {
            log.warn("Duration mismatch for rc={}: got {}s expected ~{}s", rc.infoHash(), (int) duration, (int) expected);
            return false;
          }
        }
        runtimeSeconds = (int) duration;
      }

      if (releaseRepository.existsByInfoHash(rc.infoHash())) {
        log.info("Release already exists for rc={}", rc.infoHash());
        return true;
      }

      Release release = new Release(movie, rc.infoHash(), mediaFile.name(),
          fileStatus.fileSizeBytes(), videoCodec, rc.resolution(), rc.ripType(), rc.edition(),
          runtimeSeconds, List.of(), List.of());
      releaseRepository.save(release);

      log.info("Saved release for '{}': rc={} file={}", movie.getOriginalTitle(), rc.infoHash(), mediaFile.name());
      return true;

    } catch (Exception e) {
      log.error("processMediaFile failed for rc={}: {}", rc.infoHash(), e.getMessage());
      return false;
    }
  }

  /** Returns the magnet URI if the URL redirects to one, otherwise null. */
  private String resolveRedirectToMagnet(String downloadUrl) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(downloadUrl))
        .header("X-Api-Key", prowlarrApiKey)
        .GET()
        .build();
    HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    if (response.statusCode() == 301 || response.statusCode() == 302) {
      String location = response.headers().firstValue("Location").orElse(null);
      if (location != null && location.startsWith("magnet:")) {
        return location;
      }
    }
    return null;
  }

  private byte[] fetchTorrentBytes(String downloadUrl) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(downloadUrl))
        .header("X-Api-Key", prowlarrApiKey)
        .GET()
        .build();
    HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    byte[] bytes = response.body();
    if (bytes == null || bytes.length == 0) {
      throw new IllegalStateException("Empty response fetching torrent from: " + downloadUrl);
    }
    return bytes;
  }

  private boolean validateMetadata(String metadataJson, Movie movie, ReleaseCandidate rc) {
    try {
      com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(metadataJson);
      com.fasterxml.jackson.databind.JsonNode streams = root.path("streams");
      if (streams.isEmpty() || !"video".equals(streams.get(0).path("codec_type").asText())) {
        log.warn("First stream is not video for rc={}", rc.infoHash());
        return false;
      }
      boolean hasAudio = false;
      for (com.fasterxml.jackson.databind.JsonNode s : streams) {
        if ("audio".equals(s.path("codec_type").asText())) { hasAudio = true; break; }
      }
      if (!hasAudio) {
        log.warn("No audio stream for rc={}", rc.infoHash());
        return false;
      }
      String durationStr = root.path("format").path("duration").asText(null);
      if (durationStr != null && movie.getRuntimeMinutes() != null) {
        double duration = Double.parseDouble(durationStr);
        double expected = movie.getRuntimeMinutes() * 60.0;
        if (Math.abs(duration - expected) > 0.2 * expected) {
          log.warn("Duration mismatch for rc={}: got {}s expected ~{}s", rc.infoHash(), (int) duration, (int) expected);
          return false;
        }
      }
      return true;
    } catch (Exception e) {
      log.warn("Failed to parse metadata for rc={}: {}", rc.infoHash(), e.getMessage());
      return false;
    }
  }

  private void deleteTorrentDownloadQuietly(String torrentDownloadId, String infoHash) {
    try {
      grabberrClient.deleteTorrentDownload(torrentDownloadId);
    } catch (Exception e) {
      log.error("Failed to delete torrent download for rc={}: {}", infoHash, e.getMessage());
    }
  }

  private void unclaimFileQuietly(String torrentDownloadId, int fileIndex, String infoHash) {
    try {
      grabberrClient.unclaimFile(torrentDownloadId, fileIndex);
    } catch (Exception e) {
      log.error("Failed to unclaim file for rc={}: {}", infoHash, e.getMessage());
    }
  }

  private Comparator<ReleaseCandidate> releaseCandidateComparator() {
    return (a, b) -> {
      if (a.ripType().isLowQuality() || b.ripType().isLowQuality()) {
        int cmp = Integer.compare(a.ripType().quality(), b.ripType().quality());
        if (cmp != 0) return -cmp;
      }
      int resCmp = a.resolution().compareTo(b.resolution());
      if (resCmp != 0) return -resCmp;
      int ripCmp = Integer.compare(a.ripType().quality(), b.ripType().quality());
      if (ripCmp != 0) return -ripCmp;
      if (a.releaseAt() != null && b.releaseAt() != null) {
        long dayA = a.releaseAt().getEpochSecond() / 86400;
        long dayB = b.releaseAt().getEpochSecond() / 86400;
        if (dayA != dayB) return Long.compare(dayB, dayA);
      }
      int seedersA = a.seeders() != null ? a.seeders() : 0;
      int seedersB = b.seeders() != null ? b.seeders() : 0;
      return Integer.compare(seedersB, seedersA);
    };
  }
}
