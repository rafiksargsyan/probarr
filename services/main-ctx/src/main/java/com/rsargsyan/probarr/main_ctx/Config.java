package com.rsargsyan.probarr.main_ctx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
public class Config {

  @Value("${grabberr.base-url}")
  public String grabberrBaseUrl;

  @Value("${grabberr.api-key-id}")
  public String grabberrApiKeyId;

  @Value("${grabberr.api-key}")
  public String grabberrApiKey;

  @Value("${jackett.base-url}")
  public String jackettBaseUrl;

  @Value("${jackett.api-key:}")
  public String jackettApiKey;

  @Value("${jackett.torrent-download-timeout-seconds:10}")
  public int jackettTorrentDownloadTimeoutSeconds;

  // Shared between movies and episodes - periodically clears FILE_DOWNLOAD_FAILED blacklist
  // entries so a release that failed to download once (transient - dead seeders, a network
  // blip) gets a chance to be retried later, instead of being blacklisted forever.
  @Value("${blacklist.clear-interval-seconds:21600}")
  public int blacklistClearIntervalSeconds;

  @Value("${movie.min-bitrate-kbps}")
  public int minBitrateKbps;

  @Value("${movie.scan-interval-seconds:86400}")
  public int movieScanIntervalSeconds;

  @Value("${movie.scan-stale-timeout-seconds:3600}")
  public int movieScanStaleTimeoutSeconds;

  // Stop auto-scanning old movies that have had plenty of time to show up on indexers: once a
  // movie is both this old (by release date) and has sat in probarr this long (by creation date)
  // with nothing found, further scheduled scans are a near-certain waste. forceScan still
  // bypasses this, and a manually-triggered scan from the UI (MovieService.triggerScan) never
  // goes through this check at all - it calls scanMovie directly.
  @Value("${movie.scan-skip-after-release-months:6}")
  public int movieScanSkipAfterReleaseMonths;

  @Value("${movie.scan-skip-after-creation-months:1}")
  public int movieScanSkipAfterCreationMonths;

  @Value("${movie.scan-delay-seconds}")
  public int movieScanDelaySeconds;

  @Value("${tvshow.scan-interval-seconds:86400}")
  public int tvShowScanIntervalSeconds;

  @Value("${tvshow.scan-delay-seconds}")
  public int tvShowScanDelaySeconds;

  @Value("${tvshow.episode-scan-stale-timeout-seconds:3600}")
  public int episodeScanStaleTimeoutSeconds;

  // Same idea as the movie scan-skip rule above, applied per-episode instead of per-show since a
  // show keeps airing new episodes indefinitely: once an episode is both this old (by air date)
  // and has sat in probarr this long (by creation date) with nothing found, further scheduled
  // scans of it are a near-certain waste. Manually triggering a scan from the UI
  // (EpisodeService.triggerScan -> scanEpisode) never goes through this check at all.
  @Value("${tvshow.scan-skip-after-air-months:1}")
  public int tvShowScanSkipAfterAirMonths;

  @Value("${tvshow.scan-skip-after-creation-months:1}")
  public int tvShowScanSkipAfterCreationMonths;

  @Value("${tvshow.enrichment-interval-seconds:604800}")
  public int tvShowEnrichmentIntervalSeconds;

  @Value("${tmdb.api-key}")
  public String tmdbApiKey;

  @Value("${tvdb.api-key:}")
  public String tvdbApiKey;

  @Value("${movie.enrichment-interval-seconds:604800}")
  public int movieEnrichmentIntervalSeconds;

  @Value("${movie.max-file-size-bytes:53687091200}") // 50 GB default
  public long maxFileSizeBytes;

  @Value("${movie.queued-timeout-seconds:86400}") // 24 hours default
  public int queuedTimeoutSeconds;

  @Value("${movie.metadata-fetch-timeout-seconds:300}") // 5 min default
  public int metadataFetchTimeoutSeconds;

  @Value("${movie.file-submitted-timeout-seconds:3600}") // 1 hour default
  public int fileSubmittedTimeoutSeconds;

  @Value("${movie.file-downloading-timeout-seconds:86400}") // 24 hours default
  public int fileDownloadingTimeoutSeconds;

  @Value("${movie.file-transferring-timeout-seconds:72000}") // 20 hours default
  public int fileTransferringTimeoutSeconds;

  @Value("${movie.file-progress-observation-seconds:3600}") // 1 hour default
  public int fileProgressObservationSeconds;

  @Value("${movie.file-min-download-speed-bytes:512000}") // 500 KB/s default
  public long fileMinDownloadSpeedBytes;

  @Value("${probarr.s3.access-key-id}")
  public String s3AccessKeyId;

  @Value("${probarr.s3.secret-access-key}")
  public String s3SecretAccessKey;

  @Value("${probarr.s3.region}")
  public String s3Region;

  @Value("${probarr.s3.endpoint}")
  public String s3Endpoint;

  @Value("${probarr.s3.bucket}")
  public String s3Bucket;
}
