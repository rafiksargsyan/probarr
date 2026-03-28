package com.rsargsyan.probarr.main_ctx.adapters.driven.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.core.ports.client.TmdbClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class TmdbClientImpl implements TmdbClient {

  private final RestClient restClient;

  private final String apiKey;

  @Autowired
  public TmdbClientImpl(Config config) {
    this.apiKey = config.tmdbApiKey;
    this.restClient = RestClient.builder()
        .baseUrl("https://api.themoviedb.org/3")
        .build();
  }

  @Override
  public MovieDetails getMovieDetails(Long tmdbId, String language) {
    TmdbMovieResponse resp = restClient.get()
        .uri(b -> b.path("/movie/{id}").queryParam("api_key", apiKey).queryParam("language", language).build(tmdbId))
        .retrieve()
        .body(TmdbMovieResponse.class);
    if (resp == null) return null;
    LocalDate releaseDate = null;
    if (resp.releaseDate() != null && !resp.releaseDate().isBlank()) {
      try {
        releaseDate = LocalDate.parse(resp.releaseDate());
      } catch (Exception e) {
        log.warn("Could not parse release_date '{}' for tmdbId={}", resp.releaseDate(), tmdbId);
      }
    }
    return new MovieDetails(resp.title(), resp.runtime(), releaseDate, resp.imdbId());
  }

  @Override
  public List<AlternativeTitle> getAlternativeTitles(Long tmdbId) {
    TmdbAltTitlesResponse resp = restClient.get()
        .uri(b -> b.path("/movie/{id}/alternative_titles").queryParam("api_key", apiKey).build(tmdbId))
        .retrieve()
        .body(TmdbAltTitlesResponse.class);
    if (resp == null || resp.titles() == null) return List.of();
    return resp.titles().stream()
        .map(t -> new AlternativeTitle(t.title(), t.type()))
        .toList();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TmdbMovieResponse(
      @JsonProperty("title") String title,
      @JsonProperty("runtime") Integer runtime,
      @JsonProperty("release_date") String releaseDate,
      @JsonProperty("imdb_id") String imdbId
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TmdbAltTitlesResponse(
      @JsonProperty("titles") List<TmdbAltTitle> titles
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TmdbAltTitle(
      @JsonProperty("title") String title,
      @JsonProperty("type") String type
  ) {}
}
