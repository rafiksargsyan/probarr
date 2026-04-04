package com.rsargsyan.probarr.main_ctx.adapters.driven.tvdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.core.ports.client.TvdbClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TvdbClientImpl implements TvdbClient {

  private static final String BASE_URL = "https://api4.thetvdb.com/v4";
  // Languages to try fetching translations for
  private static final List<String> NAME_LANGUAGES = List.of("eng", "rus", "spa", "fra", "ita");

  private final String apiKey;
  private final RestClient restClient;

  private volatile String authToken;
  private volatile long tokenExpiresAtEpochSeconds = 0;

  @Autowired
  public TvdbClientImpl(Config config) {
    this.apiKey = config.tvdbApiKey;
    this.restClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .build();
  }

  @Override
  public TvShow getTvShowById(Long tvdbId) {
    ensureAuthenticated();
    TvdbSeriesResponse resp = restClient.get()
        .uri("/series/{id}", tvdbId)
        .header("Authorization", "Bearer " + authToken)
        .retrieve()
        .body(TvdbSeriesResponse.class);
    if (resp == null || resp.data() == null) return null;
    TvdbSeriesData d = resp.data();
    List<String> nameTranslations = d.nameTranslations() != null ? d.nameTranslations() : List.of();
    return new TvShow(tvdbId, d.name(), nameTranslations);
  }

  @Override
  public String getTvShowTranslation(Long tvdbId, String lang) {
    ensureAuthenticated();
    try {
      TvdbTranslationResponse resp = restClient.get()
          .uri("/series/{id}/translations/{lang}", tvdbId, lang)
          .header("Authorization", "Bearer " + authToken)
          .retrieve()
          .body(TvdbTranslationResponse.class);
      if (resp == null || resp.data() == null) return null;
      return resp.data().name();
    } catch (Exception e) {
      log.warn("TVDB translation not available for tvdbId={} lang={}: {}", tvdbId, lang, e.getMessage());
      return null;
    }
  }

  @Override
  public List<TvdbEpisode> getTvShowEpisodes(Long tvdbId) {
    ensureAuthenticated();
    List<TvdbEpisode> result = new ArrayList<>();
    int page = 0;
    while (true) {
      final int p = page;
      TvdbEpisodesResponse resp = restClient.get()
          .uri(b -> b.path("/series/{id}/episodes/default").queryParam("page", p).build(tvdbId))
          .header("Authorization", "Bearer " + authToken)
          .retrieve()
          .body(TvdbEpisodesResponse.class);

      if (resp == null || resp.data() == null || resp.data().episodes() == null
          || resp.data().episodes().isEmpty()) break;

      for (TvdbEpisodeData e : resp.data().episodes()) {
        LocalDate aired = null;
        if (e.aired() != null && !e.aired().isBlank()) {
          try { aired = LocalDate.parse(e.aired()); } catch (Exception ignored) {}
        }
        result.add(new TvdbEpisode(e.id(), e.seasonNumber(), e.number(), e.runtime(), aired));
      }

      // TVDB paginates; stop when we get fewer than a full page (500 episodes/page)
      if (resp.data().episodes().size() < 500) break;
      page++;
    }
    return result;
  }

  private synchronized void ensureAuthenticated() {
    long nowSeconds = Instant.now().getEpochSecond();
    // Refresh if token is missing or expires within 1 hour
    if (authToken == null || tokenExpiresAtEpochSeconds - nowSeconds < 3600) {
      login();
    }
  }

  private void login() {
    TvdbLoginResponse resp = restClient.post()
        .uri("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("apikey", apiKey))
        .retrieve()
        .body(TvdbLoginResponse.class);
    if (resp == null || resp.data() == null || resp.data().token() == null) {
      throw new IllegalStateException("TVDB login failed — no token returned");
    }
    this.authToken = resp.data().token();
    // TVDB tokens are valid for 30 days; store expiry from JWT payload if available,
    // otherwise assume 29 days from now as a safe default
    this.tokenExpiresAtEpochSeconds = Instant.now().getEpochSecond() + 29L * 24 * 3600;
    log.info("TVDB authentication successful");
  }

  // --- Internal DTOs ---

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TvdbLoginResponse(@JsonProperty("data") TvdbTokenData data) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TvdbTokenData(@JsonProperty("token") String token) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TvdbSeriesResponse(@JsonProperty("data") TvdbSeriesData data) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TvdbSeriesData(
      @JsonProperty("name") String name,
      @JsonProperty("nameTranslations") List<String> nameTranslations
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TvdbTranslationResponse(@JsonProperty("data") TvdbTranslationData data) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TvdbTranslationData(@JsonProperty("name") String name) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TvdbEpisodesResponse(@JsonProperty("data") TvdbEpisodesData data) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TvdbEpisodesData(@JsonProperty("episodes") List<TvdbEpisodeData> episodes) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TvdbEpisodeData(
      @JsonProperty("id") Long id,
      @JsonProperty("seasonNumber") Integer seasonNumber,
      @JsonProperty("number") Integer number,
      @JsonProperty("runtime") Integer runtime,
      @JsonProperty("aired") String aired
  ) {}
}
