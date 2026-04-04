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

  @Value("${movie.min-bitrate-kbps}")
  public int minBitrateKbps;

  @Value("${movie.scan-interval-seconds:86400}")
  public int movieScanIntervalSeconds;

  @Value("${movie.scan-delay-seconds}")
  public int movieScanDelaySeconds;

  @Value("${tvshow.scan-interval-seconds:86400}")
  public int tvShowScanIntervalSeconds;

  @Value("${tvshow.scan-delay-seconds}")
  public int tvShowScanDelaySeconds;

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
}
