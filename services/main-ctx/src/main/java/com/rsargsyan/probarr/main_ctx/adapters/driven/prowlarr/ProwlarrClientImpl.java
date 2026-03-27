package com.rsargsyan.probarr.main_ctx.adapters.driven.prowlarr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.adapters.driven.TorrentHashResolver;
import com.rsargsyan.probarr.main_ctx.core.ports.client.IndexerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@Primary
@Component("prowlarrClient")
public class ProwlarrClientImpl implements IndexerClient {

  private final RestClient restClient;
  private final Config config;

  @Autowired
  public ProwlarrClientImpl(Config config) {
    this.config = config;
    this.restClient = RestClient.builder()
        .baseUrl(config.prowlarrBaseUrl)
        .defaultHeader("X-Api-Key", config.prowlarrApiKey)
        .build();
  }

  @Override
  public List<IndexerRelease> searchMovies(String imdbId, String title) {
    UriComponentsBuilder uriBuilder = UriComponentsBuilder
        .fromPath("/api/v1/search")
        .queryParam("query", title != null ? title : "")
        .queryParam("type", "movie");

    for (String cat : config.indexerMovieCategories.split(",")) {
      uriBuilder.queryParam("categories[]", cat.trim());
    }
    if (imdbId != null && !imdbId.isBlank()) {
      uriBuilder.queryParam("imdbid", imdbId);
    }

    try {
      ProwlarrRelease[] results = restClient.get()
          .uri(uriBuilder.build().toUriString())
          .retrieve()
          .body(ProwlarrRelease[].class);

      if (results == null) return List.of();

      return java.util.Arrays.stream(results)
          .map(this::toIndexerRelease)
          .filter(Objects::nonNull)
          .toList();
    } catch (Exception e) {
      log.error("Prowlarr search failed for title='{}' imdbId='{}': {}", title, imdbId, e.getMessage());
      return List.of();
    }
  }

  private IndexerRelease toIndexerRelease(ProwlarrRelease r) {
    try {
      String downloadUrl = r.magnetUrl() != null ? r.magnetUrl() : r.downloadUrl();
      if (downloadUrl == null || downloadUrl.isBlank()) return null;
      String infoHash = r.infoHash();
      if (infoHash == null || infoHash.isBlank()) {
        infoHash = TorrentHashResolver.resolve(r.magnetUrl(), r.downloadUrl());
      }
      Instant publishDate = null;
      if (r.publishDate() != null) {
        try { publishDate = Instant.parse(r.publishDate()); } catch (Exception ignored) {}
      }
      return new IndexerRelease(r.title(), r.indexer(), r.infoUrl(), downloadUrl,
          r.magnetUrl(), infoHash, r.size(), r.seeders(), publishDate);
    } catch (Exception e) {
      log.warn("Failed to map Prowlarr result '{}': {}", r.title(), e.getMessage());
      return null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ProwlarrRelease(
      @JsonProperty("title") String title,
      @JsonProperty("indexer") String indexer,
      @JsonProperty("infoUrl") String infoUrl,
      @JsonProperty("downloadUrl") String downloadUrl,
      @JsonProperty("magnetUrl") String magnetUrl,
      @JsonProperty("infoHash") String infoHash,
      @JsonProperty("size") Long size,
      @JsonProperty("seeders") Integer seeders,
      @JsonProperty("publishDate") String publishDate
  ) {}
}
