package com.rsargsyan.probarr.main_ctx.adapters.driven.jackett;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.adapters.driven.TorrentHashResolver;
import com.rsargsyan.probarr.main_ctx.core.ports.client.IndexerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component("jackettClient")
public class JackettClientImpl implements IndexerClient {

  private final RestClient restClient;
  private final Config config;

  @Autowired
  public JackettClientImpl(Config config) {
    this.config = config;
    this.restClient = RestClient.builder()
        .baseUrl(config.jackettBaseUrl)
        .build();
  }

  @Override
  public List<IndexerRelease> searchMovies(String imdbId, String title) {
    UriComponentsBuilder uriBuilder = UriComponentsBuilder
        .fromPath("/api/v2.0/indexers/all/results")
        .queryParam("apikey", config.jackettApiKey)
        .queryParam("Query", title != null ? title : "");

    for (String cat : config.indexerMovieCategories.split(",")) {
      uriBuilder.queryParam("Category[]", cat.trim());
    }
    if (imdbId != null && !imdbId.isBlank()) {
      uriBuilder.queryParam("imdbid", imdbId);
    }

    try {
      JackettSearchResponse response = restClient.get()
          .uri(uriBuilder.build().toUriString())
          .retrieve()
          .body(JackettSearchResponse.class);

      if (response == null || response.results() == null) return List.of();

      return response.results().stream()
          .map(this::toIndexerRelease)
          .filter(Objects::nonNull)
          .toList();
    } catch (Exception e) {
      log.error("Jackett search failed for title='{}' imdbId='{}': {}", title, imdbId, e.getMessage());
      return List.of();
    }
  }

  private IndexerRelease toIndexerRelease(JackettResult r) {
    try {
      String downloadUrl = r.magnetUri() != null ? r.magnetUri() : r.link();
      if (downloadUrl == null || downloadUrl.isBlank()) return null;
      String infoHash = r.infoHash();
      if (infoHash == null || infoHash.isBlank()) {
        infoHash = TorrentHashResolver.resolve(r.magnetUri(), r.link());
      }
      Instant publishDate = null;
      if (r.publishDate() != null) {
        try { publishDate = Instant.parse(r.publishDate()); } catch (Exception ignored) {}
      }
      return new IndexerRelease(r.title(), r.tracker(), r.details(), downloadUrl,
          r.magnetUri(), infoHash, r.size(), r.seeders(), publishDate);
    } catch (Exception e) {
      log.warn("Failed to map Jackett result '{}': {}", r.title(), e.getMessage());
      return null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record JackettSearchResponse(@JsonProperty("Results") List<JackettResult> results) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record JackettResult(
      @JsonProperty("Title") String title,
      @JsonProperty("Tracker") String tracker,
      @JsonProperty("Details") String details,
      @JsonProperty("Link") String link,
      @JsonProperty("MagnetUri") String magnetUri,
      @JsonProperty("InfoHash") String infoHash,
      @JsonProperty("Size") Long size,
      @JsonProperty("Seeders") Integer seeders,
      @JsonProperty("PublishDate") String publishDate
  ) {}
}
