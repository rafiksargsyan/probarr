package com.rsargsyan.probarr.main_ctx.core.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.AudioTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.SubtitleTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.service.AudioAuthorParser;
import com.rsargsyan.probarr.main_ctx.core.domain.service.AudioVoiceTypeParser;
import com.rsargsyan.probarr.main_ctx.core.domain.service.SubsAuthorParser;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AddReleaseResult;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioVoiceType;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.BlacklistReason;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Release;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.ReleaseCandidate;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsType;
import com.rsargsyan.probarr.main_ctx.core.ports.client.GrabberrClient;
import com.rsargsyan.probarr.main_ctx.core.ports.client.ObjectStorageClient;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
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
  private final GrabberrClient grabberrClient;
  private final ObjectStorageClient objectStorageClient;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final Config config;

  @Autowired
  public MovieProcessorTransactionService(MovieRepository movieRepository,
                                          GrabberrClient grabberrClient,
                                          ObjectStorageClient objectStorageClient,
                                          ObjectMapper objectMapper,
                                          Config config) {
    this.movieRepository = movieRepository;
    this.grabberrClient = grabberrClient;
    this.objectStorageClient = objectStorageClient;
    this.objectMapper = objectMapper;
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
    for (int i = 0; i < candidates.size(); i++) {
      ReleaseCandidate rc = candidates.get(i);
      if (movie.isBlacklisted(rc.infoHash())) continue;
      if (movie.isWhiteListed(rc.infoHash())) continue;
      if (movie.isOnCoolDown(rc.infoHash())) continue;
      if (rc.tracker() != null && rc.tracker().isLanguageSpecific()) {
        boolean betterAlreadyAccepted = false;
        for (int j = 0; j < i; j++) {
          ReleaseCandidate prevRc = candidates.get(j);
          if (rc.tracker() == prevRc.tracker()
              && movie.isWhiteListed(prevRc.infoHash())) {
            betterAlreadyAccepted = true;
            break;
          }
        }
        if (betterAlreadyAccepted) {
          log.debug("Blacklisting rc={} for movie='{}': superseded by better release from same tracker",
              rc.infoHash(), movie.getOriginalTitle());
          movie.addToBlackList(rc.infoHash(), BlacklistReason.SUPERSEDED);
          continue;
        }
      }

      try {
        // Once we know grabberr's own id for this candidate's torrent, always look it up by id -
        // grabberr's resolved infoHash (known only after magnet/torrent resolution) can differ
        // from what the indexer originally reported at search time, which would otherwise make
        // the candidate permanently unfindable via findByInfoHash after a successful submission.
        Optional<GrabberrClient.TorrentDownloadDTO> torrentOpt = rc.torrentDownloadId() != null
            ? grabberrClient.findById(rc.torrentDownloadId())
            : grabberrClient.findByInfoHash(rc.infoHash());

        if (torrentOpt.isEmpty()) {
          log.info("Submitting torrent for rc={} movie='{}'", rc.infoHash(), movie.getOriginalTitle());
          try {
            GrabberrClient.TorrentDownloadDTO submitted;
            if (rc.downloadUrl().startsWith("magnet:")) {
              submitted = grabberrClient.submitTorrent(rc.downloadUrl());
            } else {
              String magnetOrNull = resolveRedirectToMagnet(rc.downloadUrl());
              if (magnetOrNull != null) {
                submitted = grabberrClient.submitTorrent(magnetOrNull);
              } else {
                byte[] torrentBytes = fetchTorrentBytes(rc.downloadUrl());
                submitted = grabberrClient.submitTorrentFile(torrentBytes);
              }
            }
            movie.updateReleaseCandidateTorrentId(rc.infoHash(), submitted.id());
          } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("Permanent error submitting torrent rc={}: {}, adding to cool-down", rc.infoHash(), e.getMessage());
            movie.addToCoolDown(rc.infoHash());
          } catch (Exception e) {
            log.warn("Failed to submit torrent rc={}: {}, will retry next cycle", rc.infoHash(), e.getMessage());
          }
          shouldBreak = true;
        } else {
          if (rc.torrentDownloadId() == null) {
            movie.updateReleaseCandidateTorrentId(rc.infoHash(), torrentOpt.get().id());
          }
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
                boolean timedOut = isFileDownloadTimedOut(fileStatus, mediaFile.sizeBytes());
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
        || movie.isWhiteListed(rc.infoHash())
        || movie.isOnCoolDown(rc.infoHash()));
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

  private boolean isFileDownloadTimedOut(GrabberrClient.FileDownloadDTO fileStatus, long fileSizeBytes) {
    if (fileStatus.status() == GrabberrClient.FileDownloadStatus.SUBMITTED) {
      return fileStatus.createdAt() != null
          && Instant.now().isAfter(fileStatus.createdAt().plusSeconds(config.fileSubmittedTimeoutSeconds));
    }

    if (fileStatus.status() == GrabberrClient.FileDownloadStatus.TRANSFERRING) {
      Instant baseline = fileStatus.transferringStartedAt() != null
          ? fileStatus.transferringStartedAt()
          : fileStatus.createdAt();
      if (baseline == null) return false;
      long elapsedSeconds = Instant.now().getEpochSecond() - baseline.getEpochSecond();
      if (elapsedSeconds > config.fileTransferringTimeoutSeconds) {
        log.warn("File transfer timed out after {}s (timeout {}s)", elapsedSeconds, config.fileTransferringTimeoutSeconds);
        return true;
      }
      return false;
    }

    // DOWNLOADING — use downloadingAt, fall back to createdAt
    Instant baseline = fileStatus.downloadingAt() != null
        ? fileStatus.downloadingAt()
        : fileStatus.createdAt();
    if (baseline == null) return false;

    long elapsedSeconds = Instant.now().getEpochSecond() - baseline.getEpochSecond();

    if (elapsedSeconds > config.fileDownloadingTimeoutSeconds) return true;

    // After minimum observation period, check actual download speed
    if (elapsedSeconds >= config.fileProgressObservationSeconds) {
      float progress = fileStatus.progress() != null ? fileStatus.progress() : 0f;
      long downloadedBytes = (long) (progress * fileSizeBytes);
      long speedBytesPerSecond = downloadedBytes / elapsedSeconds;
      if (speedBytesPerSecond < config.fileMinDownloadSpeedBytes) {
        log.warn("File download too slow: {} B/s (min {} B/s) after {}s",
            speedBytesPerSecond, config.fileMinDownloadSpeedBytes, elapsedSeconds);
        return true;
      }
    }

    return false;
  }

  private long scoreFileName(String fileName, Movie movie) {
    String name = fileName.toLowerCase();

    long yearBonus = 0;
    for (Integer year : movie.getAllReleaseYears()) {
      String yearStr = String.valueOf(year);
      int idx = 0;
      while ((idx = name.indexOf(yearStr, idx)) >= 0) { yearBonus++; idx++; }
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
        log.warn("ffprobe failed (exit={}) for rc={}: {}", exitCode, rc.infoHash(), output.strip());
        return false;
      }

      JsonNode root = objectMapper.readTree(output);
      JsonNode streams = root.path("streams");
      JsonNode format = root.path("format");

      if (streams.isEmpty() || !"video".equals(streams.get(0).path("codec_type").asText())) {
        log.warn("First stream is not video for rc={}", rc.infoHash());
        return false;
      }

      JsonNode videoStream = streams.get(0);
      Integer width = videoStream.hasNonNull("width") ? videoStream.get("width").asInt() : null;
      Integer height = videoStream.hasNonNull("height") ? videoStream.get("height").asInt() : null;

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

      List<AudioTrack> audioTracks = extractAudioTracks(streams, movie.getOriginalLocale());
      if (audioTracks.isEmpty()) {
        log.warn("No audio tracks with detectable language for rc={}, blacklisting", rc.infoHash());
        movie.addToBlackList(rc.infoHash(), BlacklistReason.NO_AUDIO_TRACKS);
        return false;
      }
      List<SubtitleTrack> subtitleTracks = extractSubtitleTracks(streams, movie.getOriginalLocale());

      String torrentSource = resolveTorrentSource(rc.infoHash());

      Release release = new Release(rc.infoHash(), mediaFile.name(),
          fileStatus.fileSizeBytes(), rc.resolution(), width, height, rc.ripType(), rc.edition(),
          runtimeSeconds, audioTracks, subtitleTracks, Instant.now(), torrentSource, mediaFile.index(), null);

      AddReleaseResult result = movie.addRelease(release);
      log.info("Release for '{}' rc={}: {}", movie.getOriginalTitle(), rc.infoHash(),
          result.accepted() ? "accepted" : "rejected by comparison");
      for (Release replaced : result.replacedReleases()) {
        unclaimReplacedRelease(replaced);
      }
      return result.accepted();

    } catch (Exception e) {
      log.error("processMediaFile failed for rc={}: {}", rc.infoHash(), e.getMessage());
      return false;
    }
  }

  // No fallback to null here on purpose: a Release with no torrentSource is unusable (q62 can
  // never grab it), so a failure to resolve it must abort the whole release creation instead of
  // silently producing a broken Release - propagates to processMediaFile's own catch, which
  // rejects the file and lets it be retried next cycle rather than getting stuck half-created.
  private String resolveTorrentSource(String infoHash) throws IOException, InterruptedException {
    GrabberrClient.TorrentSourceDTO sourceDto = grabberrClient.getTorrentSourceByHash(infoHash);
    String value = sourceDto.value();
    if (value == null) {
      throw new IllegalStateException("Grabberr returned no torrent source for infoHash=" + infoHash);
    }
    if (value.startsWith("magnet:")) return value;
    // It's a signed URL to grabberr's S3 — download and re-upload to probarr's S3
    String s3Key = "torrents/" + infoHash + ".torrent";
    byte[] torrentBytes = fetchTorrentBytes(value);
    objectStorageClient.upload(s3Key, torrentBytes, "application/x-bittorrent");
    return s3Key;
  }

  private List<AudioTrack> extractAudioTracks(JsonNode streams, Locale originalLocale) {
    int numUndefinedAudio = 0;
    for (JsonNode s : streams) {
      if (!"audio".equals(s.path("codec_type").asText())) continue;
      String lc = s.path("tags").path("language").asText(null);
      if (lc == null || "und".equals(lc)) numUndefinedAudio++;
    }
    List<AudioTrack> result = new ArrayList<>();
    for (JsonNode s : streams) {
      if (!"audio".equals(s.path("codec_type").asText())) continue;
      int channels = s.path("channels").asInt(0);
      String langCode = s.path("tags").path("language").asText(null);
      if ((langCode == null || "und".equals(langCode)) && numUndefinedAudio > 1) continue;
      String streamTitle = s.path("tags").path("title").asText(null);
      Locale language = Locale.fromISO639_2(langCode).orElse(null);
      AudioAuthor author = AudioAuthorParser.parse(streamTitle);
      AudioVoiceType voiceType = AudioVoiceTypeParser.parse(streamTitle, author, language, originalLocale);
      if (voiceType == AudioVoiceType.ORIGINAL && originalLocale != null) {
        language = originalLocale;
      } else if (language != null && originalLocale != null) {
        String detectedTag = language.getTag();
        String originalTag = originalLocale.getTag();
        if (originalTag.startsWith(detectedTag + "-")) language = originalLocale;
      }
      if (language != null && streamTitle != null) language = refineLocaleFromTitle(language, streamTitle);
      if (language == null) continue;
      addAudioTrack(result, new AudioTrack(s.path("index").asInt(), language, channels == 0 ? null : channels, voiceType, author));
    }
    return result;
  }

  private static Locale refineLocaleFromTitle(Locale locale, String streamTitle) {
    String t = streamTitle.toLowerCase();
    if (locale == Locale.ES) {
      if (t.contains("latin")) return Locale.ES_419;
      if (t.contains("spain")) return Locale.ES_ES;
    }
    if (locale == Locale.FR) {
      if (t.contains("canad") || t.contains("vfq")) return Locale.FR_CA;
      if (t.contains("france") || t.contains("vff")) return Locale.FR_FR;
    }
    if (locale == Locale.PT) {
      if (t.contains("brazil") || streamTitle.contains("BR")) return Locale.PT_BR;
      if (t.contains("portugal")) return Locale.PT_PT;
    }
    return locale;
  }

  private static void addAudioTrack(List<AudioTrack> tracks, AudioTrack track) {
    if (track.voiceType() == AudioVoiceType.COMMENTARY) {
      boolean hasCommentaryForLang = tracks.stream()
          .anyMatch(t -> t.voiceType() == AudioVoiceType.COMMENTARY
              && java.util.Objects.equals(t.language(), track.language()));
      if (!hasCommentaryForLang) tracks.add(track);
      return;
    }
    for (AudioTrack existing : tracks) {
      Integer cmp = Release.compareAudio(existing, track);
      if (cmp != null && cmp >= 0) return;
    }
    tracks.removeIf(existing -> Release.compareAudio(existing, track) != null);
    tracks.add(track);
  }

  private List<SubtitleTrack> extractSubtitleTracks(JsonNode streams, Locale originalLocale) {
    List<SubtitleTrack> result = new ArrayList<>();
    for (JsonNode s : streams) {
      if (!"subtitle".equals(s.path("codec_type").asText())) continue;
      String codecName = s.path("codec_name").asText(null);
      if (!"subrip".equals(codecName) && !"ass".equals(codecName) && !"webvtt".equals(codecName)) continue;
      String langCode = s.path("tags").path("language").asText(null);
      String streamTitle = s.path("tags").path("title").asText(null);
      Locale language = Locale.fromISO639_2(langCode).orElse(null);
      if (language != null && originalLocale != null) {
        String detectedTag = language.getTag();
        String originalTag = originalLocale.getTag();
        if (originalTag.startsWith(detectedTag + "-")) language = originalLocale;
      }
      if (language != null && streamTitle != null) language = refineLocaleFromTitle(language, streamTitle);
      SubsAuthor author = SubsAuthorParser.parse(streamTitle);
      SubsType subsType = SubsType.fromTitle(streamTitle);
      addSubtitleTrack(result, new SubtitleTrack(s.path("index").asInt(), language, subsType, author));
    }
    return result;
  }

  private static void addSubtitleTrack(List<SubtitleTrack> tracks, SubtitleTrack track) {
    for (SubtitleTrack existing : tracks) {
      Integer cmp = Release.compareSubs(existing, track);
      if (cmp != null && cmp >= 0) return;
    }
    tracks.removeIf(existing -> {
      Integer cmp = Release.compareSubs(existing, track);
      return cmp != null && cmp < 0;
    });
    tracks.add(track);
  }

  /** Returns the magnet URI if the URL redirects to one, otherwise null. */
  private String resolveRedirectToMagnet(String downloadUrl) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(downloadUrl))
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
        .GET()
        .build();
    HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() == 404) {
      // Jackett's download-link cache entry has expired/is gone - retrying the same URL can
      // never succeed, so treat it as permanent (routes to cool-down, not infinite retry).
      throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null);
    }
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

  private void unclaimReplacedRelease(Release replaced) {
    try {
      grabberrClient.findByInfoHash(replaced.infoHash())
          .ifPresentOrElse(
              torrent -> unclaimFileQuietly(torrent.id(), replaced.fileIndex(), replaced.infoHash()),
              () -> log.warn("No grabberr torrent found for replaced release rc={}, cannot unclaim",
                  replaced.infoHash()));
    } catch (Exception e) {
      log.error("Failed to resolve grabberr torrent for replaced release rc={}: {}", replaced.infoHash(), e.getMessage());
    }
  }

  private Comparator<ReleaseCandidate> releaseCandidateComparator() {
    Comparator<ReleaseCandidate> byRipQualityDesc =
        Comparator.comparingInt((ReleaseCandidate rc) -> rc.ripType().quality()).reversed();
    Comparator<ReleaseCandidate> byResolutionDesc =
        Comparator.comparing(ReleaseCandidate::resolution).reversed();
    // Missing release date sorts as "oldest" via a fixed sentinel, not a per-pair tie-and-defer-
    // to-seeders rule - that earlier version decided whether *date* or *seeders* was the deciding
    // factor based on which specific pair was being compared (tie only when *this* pair had a
    // missing date), which is the exact same non-transitive pattern as the rip-quality/resolution
    // bug above, just relocated: three candidates with recent+low-seeders, no-date+mid-seeders,
    // and old+high-seeders dates can cycle. A fixed sentinel makes this a pure per-element key.
    Comparator<ReleaseCandidate> byDayDesc = Comparator.comparingLong(
        (ReleaseCandidate rc) -> rc.releaseAt() != null ? rc.releaseAt().getEpochSecond() / 86400 : Long.MIN_VALUE)
        .reversed();
    Comparator<ReleaseCandidate> bySeedersDesc =
        Comparator.comparingInt((ReleaseCandidate rc) -> rc.seeders() != null ? rc.seeders() : 0).reversed();

    return Comparator
        // Low-quality (CAM/TELESYNC) candidates are grouped and ranked separately from everything
        // else: resolution numbers on a cam-rip aren't meaningful, so rip-type quality leads
        // within that group instead. This must be the top-level key, not a per-pair check like
        // "either side is low quality" (the previous version) - deciding which field to compare
        // by based on a property of the *pair* rather than each element on its own is exactly
        // what makes a comparator non-transitive and trips TimSort's contract check. Branching on
        // a.ripType().isLowQuality() in the next step is safe only because thenComparing never
        // even calls it unless this key already proved a and b share the same isLowQuality value.
        .comparing((ReleaseCandidate rc) -> rc.ripType().isLowQuality())
        .thenComparing((a, b) -> a.ripType().isLowQuality()
            ? byRipQualityDesc.compare(a, b)
            : byResolutionDesc.compare(a, b))
        .thenComparing(byRipQualityDesc)
        .thenComparing(byDayDesc)
        .thenComparing(bySeedersDesc);
  }
}
