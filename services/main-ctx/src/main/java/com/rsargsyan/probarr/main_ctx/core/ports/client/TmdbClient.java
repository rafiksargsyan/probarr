package com.rsargsyan.probarr.main_ctx.core.ports.client;

import java.time.LocalDate;
import java.util.List;

public interface TmdbClient {

  MovieDetails getMovieDetails(Long tmdbId, String language);

  List<AlternativeTitle> getAlternativeTitles(Long tmdbId);

  TvShowDetails getTvShowDetails(Long tmdbId, String language);

  TvShowExternalIds getTvShowExternalIds(Long tmdbId);

  record TvShowExternalIds(Long tvdbId, String imdbId) {}

  List<SeasonEpisodeDetails> getSeasonEpisodes(Long tmdbId, int seasonNumber);

  record MovieDetails(String title, Integer runtimeMinutes, LocalDate releaseDate, String imdbId) {}

  record AlternativeTitle(String title, String type) {}

  record TvShowDetails(String name, LocalDate firstAirDate, List<Integer> seasonNumbers) {}

  record SeasonEpisodeDetails(int episodeNumber, LocalDate airDate, Integer runtimeMinutes) {}
}
