package com.rsargsyan.probarr.main_ctx.core.ports.client;

import java.time.LocalDate;
import java.util.List;

public interface TmdbClient {

  MovieDetails getMovieDetails(Long tmdbId, String language);

  List<AlternativeTitle> getAlternativeTitles(Long tmdbId);

  record MovieDetails(String title, Integer runtimeMinutes, LocalDate releaseDate, String imdbId) {}

  record AlternativeTitle(String title, String type) {}
}
