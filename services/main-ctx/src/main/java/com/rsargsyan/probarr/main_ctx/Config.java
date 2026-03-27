package com.rsargsyan.probarr.main_ctx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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

  @Value("${prowlarr.base-url}")
  public String prowlarrBaseUrl;

  @Value("${prowlarr.api-key:}")
  public String prowlarrApiKey;

  @Value("${indexer.movie-categories:2000,2040,2045,2060}")
  public String indexerMovieCategories;

  @Value("${movie.scan-interval-hours:24}")
  public int movieScanIntervalHours;
}
