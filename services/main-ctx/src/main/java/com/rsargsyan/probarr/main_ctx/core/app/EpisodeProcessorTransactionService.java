package com.rsargsyan.probarr.main_ctx.core.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Episode;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.AudioTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.SubtitleTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.service.AudioAuthorParser;
import com.rsargsyan.probarr.main_ctx.core.domain.service.AudioVoiceTypeParser;
import com.rsargsyan.probarr.main_ctx.core.domain.service.SubsAuthorParser;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioVoiceType;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.BlacklistReason;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Language;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Release;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.ReleaseCandidate;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsType;
import com.rsargsyan.probarr.main_ctx.core.ports.client.GrabberrClient;
import com.rsargsyan.probarr.main_ctx.core.ports.client.ObjectStorageClient;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.EpisodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EpisodeProcessorTransactionService {

  private final EpisodeRepository episodeRepository;
  private final GrabberrClient grabberrClient;
  private final ObjectStorageClient objectStorageClient;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final Config config;

  @Autowired
  public EpisodeProcessorTransactionService(EpisodeRepository episodeRepository,
                                             GrabberrClient grabberrClient,
                                             ObjectStorageClient objectStorageClient,
                                             ObjectMapper objectMapper,
                                             Config config) {
    this.episodeRepository = episodeRepository;
    this.grabberrClient = grabberrClient;
    this.objectStorageClient = objectStorageClient;
    this.objectMapper = objectMapper;
    this.config = config;
    this.httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  @Transactional
  public void processEpisode(Long episodeId) {
    Episode episode = episodeRepository.findById(episodeId)
        .orElseThrow(() -> new IllegalArgumentException("Episode not found: " + episodeId));

    if (episode.getReleaseCandidates().isEmpty()) return;

    List<ReleaseCandidate> candidates = episode.getReleaseCandidates().stream()
        .sorted(releaseCandidateComparator())
        .toList();

    String label = episodeLabel(episode);
    boolean shouldBreak = false;

    for (ReleaseCandidate rc : candidates) {
      if (episode.isBlacklisted(rc.infoHash())) continue;
      if (episode.getWhiteList().contains(rc.infoHash())) continue;
      if (episode.getCoolDownList().contains(rc.infoHash())) continue;
      if (isOldGenericRelease(rc, episode.getAirDate(), episode.getTvShow().getOriginalLocale())) continue;

      try {
        Optional<GrabberrClient.TorrentDownloadDTO> torrentOpt = grabberrClient.findByInfoHash(rc.infoHash());

        if (torrentOpt.isEmpty()) {
          log.info("Submitting torrent for rc={} episode='{}'", rc.infoHash(), label);
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
            episode.addToCoolDown(rc.infoHash());
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
              episode.addToCoolDown(rc.infoHash());
            } else {
              shouldBreak = true;
            }
          } else if (torrent.status() == GrabberrClient.TorrentStatus.FAILED) {
            log.warn("Torrent rc={} failed metadata fetch, blacklisting", rc.infoHash());
            episode.addToBlackList(rc.infoHash(), BlacklistReason.TORRENT_FAILED);
            deleteTorrentDownloadQuietly(torrent.id(), rc.infoHash());
          } else { // READY
            GrabberrClient.TorrentFile mediaFile = findMediaFile(torrent.files(), episode, episode.getTvShow().getNames());
            if (mediaFile == null) {
              log.warn("No media file found for rc={} episode='{}', blacklisting", rc.infoHash(), label);
              episode.addToBlackList(rc.infoHash(), BlacklistReason.NO_MEDIA_FILE);
              deleteTorrentDownloadQuietly(torrent.id(), rc.infoHash());
            } else if (mediaFile.sizeBytes() > config.maxFileSizeBytes) {
              log.warn("Media file too large ({} bytes) for rc={}, blacklisting",
                  mediaFile.sizeBytes(), rc.infoHash());
              episode.addToBlackList(rc.infoHash(), BlacklistReason.FILE_TOO_LARGE);
              deleteTorrentDownloadQuietly(torrent.id(), rc.infoHash());
            } else {
              GrabberrClient.FileDownloadDTO fileStatus = grabberrClient.getFileStatus(torrent.id(), mediaFile.index());
              if (fileStatus == null) {
                log.info("Claiming file index={} for rc={}", mediaFile.index(), rc.infoHash());
                grabberrClient.claimFile(torrent.id(), mediaFile.index());
                shouldBreak = true;
              } else if (fileStatus.status() == GrabberrClient.FileDownloadStatus.DOWNLOADED) {
                if (fileStatus.metadata() != null) {
                  if (validateMetadata(fileStatus.metadata(), episode, rc)) {
                    grabberrClient.cacheFile(torrent.id(), mediaFile.index());
                  } else {
                    episode.addToBlackList(rc.infoHash(), BlacklistReason.PROCESSING_FAILED);
                    unclaimFileQuietly(torrent.id(), mediaFile.index(), rc.infoHash());
                  }
                } else {
                  grabberrClient.cacheFile(torrent.id(), mediaFile.index());
                }
                shouldBreak = true;
              } else if (fileStatus.status() == GrabberrClient.FileDownloadStatus.DONE) {
                if (processMediaFile(episode, rc, mediaFile, fileStatus)) {
                  episode.addToWhiteList(rc.infoHash());
                } else {
                  episode.addToBlackList(rc.infoHash(), BlacklistReason.PROCESSING_FAILED);
                }
                shouldBreak = true;
              } else if (fileStatus.status() == GrabberrClient.FileDownloadStatus.FAILED) {
                log.warn("File download failed for rc={}, blacklisting", rc.infoHash());
                episode.addToBlackList(rc.infoHash(), BlacklistReason.FILE_DOWNLOAD_FAILED);
                unclaimFileQuietly(torrent.id(), mediaFile.index(), rc.infoHash());
              } else {
                boolean timedOut = isFileDownloadTimedOut(fileStatus, mediaFile.sizeBytes());
                if (timedOut) {
                  log.warn("File download timed out for rc={} status={}, unclaiming and moving to cool-down",
                      rc.infoHash(), fileStatus.status());
                  unclaimFileQuietly(torrent.id(), mediaFile.index(), rc.infoHash());
                  episode.addToCoolDown(rc.infoHash());
                } else {
                  shouldBreak = true;
                }
              }
            }
          }
        }
      } catch (ResourceAccessException e) {
        log.warn("Grabberr unreachable while processing rc={} for episode '{}': {}",
            rc.infoHash(), label, e.getMessage());
        shouldBreak = true;
      } catch (Exception e) {
        log.error("Error processing rc={} for episode '{}': {}", rc.infoHash(), label, e.getMessage());
        episode.addToBlackList(rc.infoHash(), BlacklistReason.PROCESSING_ERROR);
      }

      if (shouldBreak) break;
    }

    boolean allDone = candidates.stream().allMatch(rc ->
        episode.isBlacklisted(rc.infoHash())
        || episode.getWhiteList().contains(rc.infoHash())
        || episode.getCoolDownList().contains(rc.infoHash()));
    if (allDone) {
      log.info("All candidates processed for episode '{}', clearing RC list", label);
      episode.clearReleaseCandidates();
    }

    episodeRepository.save(episode);
  }

  /**
   * Finds the media file in the torrent that corresponds to this episode.
   * For multi-episode torrents, matches by episode number in the filename (e.g. E03, E003).
   */
  private GrabberrClient.TorrentFile findMediaFile(List<GrabberrClient.TorrentFile> files, Episode episode, List<String> showNames) {
    List<GrabberrClient.TorrentFile> videoFiles = files.stream()
        .filter(f -> {
          String name = f.name().toLowerCase();
          return (name.endsWith(".mkv") || name.endsWith(".mp4") || name.endsWith(".avi"))
              && !name.contains("sample");
        })
        .toList();

    if (videoFiles.isEmpty()) return null;

    // Single file: return it directly
    if (videoFiles.size() == 1) return videoFiles.get(0);

    Integer epNum = episode.getEpisodeNumber();
    Integer absNum = episode.getAbsoluteNumber();
    Integer seasonNum = episode.getSeasonNumber();

    if (epNum != null || absNum != null) {
      int num = epNum != null ? epNum : absNum;
      int season = seasonNum != null ? seasonNum : 1;

      // Pattern 1: s01e05 format
      Pattern sxePattern = Pattern.compile(
          String.format("s0*%de0*%d[^\\d]", season, num), Pattern.CASE_INSENSITIVE);
      // Pattern 2: 1x05 format
      Pattern nxnPattern = Pattern.compile(
          String.format("0*%dx0*%d[^\\d]", season, num), Pattern.CASE_INSENSITIVE);

      // First pass: match against full path
      List<GrabberrClient.TorrentFile> candidates = videoFiles.stream()
          .filter(f -> {
            String name = f.name().toLowerCase();
            return sxePattern.matcher(name).find() || nxnPattern.matcher(name).find();
          })
          .collect(java.util.stream.Collectors.toList());

      // Second pass: if multiple, narrow down to base filename only
      if (candidates.size() > 1) {
        candidates = candidates.stream()
            .filter(f -> {
              String name = f.name().toLowerCase();
              int slash = name.lastIndexOf('/');
              String baseName = slash >= 0 ? name.substring(slash + 1) : name;
              return sxePattern.matcher(baseName).find() || nxnPattern.matcher(baseName).find();
            })
            .collect(java.util.stream.Collectors.toList());
      }

      if (candidates.size() == 1) return candidates.get(0);

      // Fallback: <show title><whitespace><number>: "Jujutsu Kaisen 05"
      for (String showName : showNames) {
        Pattern titlePattern = Pattern.compile(
            Pattern.quote(showName) + "\\s+0*" + num + "(?!\\d)",
            Pattern.CASE_INSENSITIVE);
        List<GrabberrClient.TorrentFile> titleMatched = videoFiles.stream()
            .filter(f -> titlePattern.matcher(f.name()).find())
            .toList();
        if (titleMatched.size() == 1) return titleMatched.get(0);
      }
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
      return elapsedSeconds > config.fileTransferringTimeoutSeconds;
    }

    Instant baseline = fileStatus.downloadingAt() != null
        ? fileStatus.downloadingAt()
        : fileStatus.createdAt();
    if (baseline == null) return false;

    long elapsedSeconds = Instant.now().getEpochSecond() - baseline.getEpochSecond();

    if (elapsedSeconds > config.fileDownloadingTimeoutSeconds) return true;

    if (elapsedSeconds >= config.fileProgressObservationSeconds) {
      float progress = fileStatus.progress() != null ? fileStatus.progress() : 0f;
      long downloadedBytes = (long) (progress * fileSizeBytes);
      long speedBytesPerSecond = downloadedBytes / elapsedSeconds;
      if (speedBytesPerSecond < config.fileMinDownloadSpeedBytes) {
        log.warn("File download too slow: {} B/s (min {} B/s)", speedBytesPerSecond, config.fileMinDownloadSpeedBytes);
        return true;
      }
    }

    return false;
  }

  private boolean processMediaFile(Episode episode, ReleaseCandidate rc,
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
        if (episode.getRuntimeSeconds() != null && episode.getRuntimeSeconds() > 0) {
          double expected = episode.getRuntimeSeconds();
          if (Math.abs(duration - expected) > 0.25 * expected) {
            log.warn("Duration mismatch for rc={}: got {}s expected ~{}s", rc.infoHash(), (int) duration, (int) expected);
            return false;
          }
        }
        runtimeSeconds = (int) duration;
      }

      List<AudioTrack> audioTracks = extractAudioTracks(streams, episode.getTvShow().getOriginalLocale());
      List<SubtitleTrack> subtitleTracks = extractSubtitleTracks(streams, episode.getTvShow().getOriginalLocale());

      String torrentSource = resolveTorrentSource(rc.infoHash());

      Release release = new Release(rc.infoHash(), mediaFile.name(),
          fileStatus.fileSizeBytes(), rc.resolution(), width, height, rc.ripType(), null,
          runtimeSeconds, audioTracks, subtitleTracks, Instant.now(), torrentSource, mediaFile.index());

      boolean accepted = episode.addRelease(release);
      log.info("Release for episode '{}' rc={}: {}", episodeLabel(episode), rc.infoHash(),
          accepted ? "accepted" : "rejected by comparison");
      return accepted;

    } catch (Exception e) {
      log.error("processMediaFile failed for rc={}: {}", rc.infoHash(), e.getMessage());
      return false;
    }
  }

  private boolean validateMetadata(String metadataJson, Episode episode, ReleaseCandidate rc) {
    try {
      JsonNode root = objectMapper.readTree(metadataJson);
      JsonNode streams = root.path("streams");
      if (streams.isEmpty() || !"video".equals(streams.get(0).path("codec_type").asText())) return false;
      boolean hasAudio = false;
      for (JsonNode s : streams) {
        if ("audio".equals(s.path("codec_type").asText())) { hasAudio = true; break; }
      }
      if (!hasAudio) return false;
      String durationStr = root.path("format").path("duration").asText(null);
      if (durationStr != null && episode.getRuntimeSeconds() != null && episode.getRuntimeSeconds() > 0) {
        double duration = Double.parseDouble(durationStr);
        double expected = episode.getRuntimeSeconds();
        if (Math.abs(duration - expected) > 0.25 * expected) return false;
      }
      return true;
    } catch (Exception e) {
      log.warn("Failed to parse metadata for rc={}: {}", rc.infoHash(), e.getMessage());
      return false;
    }
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

  private String resolveTorrentSource(String infoHash) {
    try {
      GrabberrClient.TorrentSourceDTO sourceDto = grabberrClient.getTorrentSourceByHash(infoHash);
      String value = sourceDto.value();
      if (value == null) return null;
      if (value.startsWith("magnet:")) return value;
      String s3Key = "torrents/" + infoHash + ".torrent";
      byte[] torrentBytes = fetchTorrentBytes(value);
      objectStorageClient.upload(s3Key, torrentBytes, "application/x-bittorrent");
      return s3Key;
    } catch (Exception e) {
      log.warn("Could not resolve torrent source for infoHash={}: {}", infoHash, e.getMessage());
      return null;
    }
  }

  private String resolveRedirectToMagnet(String downloadUrl) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(downloadUrl))
        .GET()
        .build();
    HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    if (response.statusCode() == 301 || response.statusCode() == 302) {
      String location = response.headers().firstValue("Location").orElse(null);
      if (location != null && location.startsWith("magnet:")) return location;
    }
    return null;
  }

  private byte[] fetchTorrentBytes(String downloadUrl) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(downloadUrl))
        .GET()
        .build();
    HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    byte[] bytes = response.body();
    if (bytes == null || bytes.length == 0) {
      throw new IllegalStateException("Empty response fetching torrent from: " + downloadUrl);
    }
    return bytes;
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

  private static boolean isOldGenericRelease(ReleaseCandidate rc, LocalDate contentDate, Locale originalLocale) {
    if (rc.tracker() == null || rc.tracker().isLanguageSpecific()) return false;
    if (contentDate == null) return false;
    if (!contentDate.isBefore(LocalDate.now().minusMonths(3))) return false;
    return isGenericLanguageRelease(rc.languages(), originalLocale);
  }

  private static boolean isGenericLanguageRelease(List<Language> languages, Locale originalLocale) {
    if (languages == null || languages.isEmpty()) return true;
    if (languages.size() == 1 && languages.get(0) == Language.ORIGINAL) return true;
    if (languages.size() == 1 && originalLocale != null) return languageMatchesLocaleBase(languages.get(0), originalLocale);
    return false;
  }

  private static boolean languageMatchesLocaleBase(Language lang, Locale locale) {
    String base = locale.getTag().split("-")[0];
    return switch (lang) {
      case ENGLISH -> "en".equals(base);
      case RUSSIAN -> "ru".equals(base);
      case FRENCH -> "fr".equals(base);
      case SPANISH, SPANISH_LATINO -> "es".equals(base);
      case GERMAN -> "de".equals(base);
      case ITALIAN -> "it".equals(base);
      case PORTUGUESE, PORTUGUESE_BR -> "pt".equals(base);
      case HINDI -> "hi".equals(base);
      case JAPANESE -> "ja".equals(base);
      case KOREAN -> "ko".equals(base);
      case CHINESE -> "zh".equals(base);
      case DUTCH -> "nl".equals(base);
      case NORWEGIAN -> "nb".equals(base);
      case SWEDISH -> "sv".equals(base);
      case DANISH -> "da".equals(base);
      case POLISH -> "pl".equals(base);
      case UKRAINIAN -> "uk".equals(base);
      case ROMANIAN -> "ro".equals(base);
      case CZECH -> "cs".equals(base);
      case HUNGARIAN -> "hu".equals(base);
      case BULGARIAN -> "bg".equals(base);
      case SLOVAK -> "sk".equals(base);
      case LITHUANIAN -> "lt".equals(base);
      case ARABIC -> "ar".equals(base);
      case HEBREW -> "he".equals(base);
      case TURKISH -> "tr".equals(base);
      case GREEK -> "el".equals(base);
      case FINNISH -> "fi".equals(base);
      case VIETNAMESE -> "vi".equals(base);
      case THAI -> "th".equals(base);
      case TELUGU -> "te".equals(base);
      case URDU -> "ur".equals(base);
      default -> false;
    };
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

  private String episodeLabel(Episode episode) {
    return String.format("S%02dE%02d",
        episode.getSeasonNumber() != null ? episode.getSeasonNumber() : 0,
        episode.getEpisodeNumber() != null ? episode.getEpisodeNumber() : 0);
  }
}
