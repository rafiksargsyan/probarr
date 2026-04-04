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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
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
  public List<IndexerRelease> searchMovies(String title) {
    UriComponentsBuilder uri = UriComponentsBuilder
        .fromPath("/api/v2.0/indexers/all/results")
        .queryParam("apikey", config.jackettApiKey)
        .queryParam("Query", title)
        .queryParam("Category", 2000)
        .queryParam("Category", 8000);
    return fetch(uri, "movie title='" + title + "'");
  }

  @Override
  public List<IndexerRelease> searchTvShowSeason(String title, int seasonNumber) {
    UriComponentsBuilder uri = UriComponentsBuilder
        .fromPath("/api/v2.0/indexers/all/results")
        .queryParam("apikey", config.jackettApiKey)
        .queryParam("Query", title)
        .queryParam("Category", 5000)
        .queryParam("Category", 8000);
    return fetch(uri, "tvsearch title='" + title + "'");
  }

  private List<IndexerRelease> fetch(UriComponentsBuilder uri, String description) {
    try {
      JackettSearchResponse response = restClient.get()
          .uri(uri.build().toUriString())
          .retrieve()
          .body(JackettSearchResponse.class);
      if (response == null || response.results() == null) return List.of();
      return Arrays.stream(response.results())
          .map(this::toIndexerRelease)
          .filter(Objects::nonNull)
          .toList();
    } catch (Exception e) {
      log.error("Jackett {} failed: {}", description, e.getMessage());
      return List.of();
    }
  }

  private IndexerRelease toIndexerRelease(JackettResult r) {
    try {
      String downloadUrl = r.magnetUri() != null ? r.magnetUri() : r.link();
      if (downloadUrl == null || downloadUrl.isBlank()) return null;
      String infoHash = r.infoHash();
      if (infoHash == null || infoHash.isBlank()) {
        infoHash = TorrentHashResolver.extractFromMagnet(r.magnetUri());
      }
      if (infoHash == null || infoHash.isBlank()) {
        infoHash = TorrentHashResolver.extractFromTorrentUrl(downloadUrl);
      }
      Instant publishDate = null;
      if (r.publishDate() != null && !r.publishDate().isBlank()) {
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
  record JackettSearchResponse(@JsonProperty("Results") JackettResult[] results) {}

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
