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

  @Override
  public TvShowDetails getTvShowDetails(Long tmdbId, String language) {
    TmdbTvShowResponse resp = restClient.get()
        .uri(b -> b.path("/tv/{id}").queryParam("api_key", apiKey).queryParam("language", language).build(tmdbId))
        .retrieve()
        .body(TmdbTvShowResponse.class);
    if (resp == null) return null;
    LocalDate firstAirDate = null;
    if (resp.firstAirDate() != null && !resp.firstAirDate().isBlank()) {
      try {
        firstAirDate = LocalDate.parse(resp.firstAirDate());
      } catch (Exception e) {
        log.warn("Could not parse first_air_date '{}' for tmdbId={}", resp.firstAirDate(), tmdbId);
      }
    }
    List<Integer> seasonNumbers = resp.seasons() == null ? List.of() : resp.seasons().stream()
        .map(TmdbSeasonSummary::seasonNumber)
        .filter(n -> n != null && n > 0)
        .toList();
    return new TvShowDetails(resp.name(), firstAirDate, seasonNumbers);
  }

  @Override
  public TvShowExternalIds getTvShowExternalIds(Long tmdbId) {
    TmdbExternalIdsResponse resp = restClient.get()
        .uri(b -> b.path("/tv/{id}/external_ids").queryParam("api_key", apiKey).build(tmdbId))
        .retrieve()
        .body(TmdbExternalIdsResponse.class);
    if (resp == null) return null;
    return new TvShowExternalIds(resp.tvdbId(), resp.imdbId());
  }

  @Override
  public List<SeasonEpisodeDetails> getSeasonEpisodes(Long tmdbId, int seasonNumber) {
    TmdbSeasonResponse resp = restClient.get()
        .uri(b -> b.path("/tv/{id}/season/{season}").queryParam("api_key", apiKey).build(tmdbId, seasonNumber))
        .retrieve()
        .body(TmdbSeasonResponse.class);
    if (resp == null || resp.episodes() == null) return List.of();
    return resp.episodes().stream().map(e -> {
      LocalDate airDate = null;
      if (e.airDate() != null && !e.airDate().isBlank()) {
        try { airDate = LocalDate.parse(e.airDate()); } catch (Exception ignored) {}
      }
      return new SeasonEpisodeDetails(e.episodeNumber(), airDate, e.runtime());
    }).toList();
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TmdbTvShowResponse(
      @JsonProperty("name") String name,
      @JsonProperty("first_air_date") String firstAirDate,
      @JsonProperty("seasons") List<TmdbSeasonSummary> seasons
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TmdbSeasonSummary(
      @JsonProperty("season_number") Integer seasonNumber
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TmdbExternalIdsResponse(
      @JsonProperty("tvdb_id") Long tvdbId,
      @JsonProperty("imdb_id") String imdbId
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TmdbSeasonResponse(
      @JsonProperty("episodes") List<TmdbEpisode> episodes
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TmdbEpisode(
      @JsonProperty("episode_number") int episodeNumber,
      @JsonProperty("air_date") String airDate,
      @JsonProperty("runtime") Integer runtime
  ) {}
}
