package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import com.rsargsyan.probarr.main_ctx.core.ports.client.TmdbClient;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class MovieEnrichmentTransactionService {

  private final MovieRepository movieRepository;
  private final TmdbClient tmdbClient;

  @Autowired
  public MovieEnrichmentTransactionService(MovieRepository movieRepository, TmdbClient tmdbClient) {
    this.movieRepository = movieRepository;
    this.tmdbClient = tmdbClient;
  }

  @Transactional
  public void enrichMovie(Long movieId) {
    Movie movie = movieRepository.findById(movieId)
        .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + movieId));

    Long tmdbId = movie.getTmdbId();
    if (tmdbId == null) return;

    log.info("Enriching movie '{}' tmdbId={}", movie.getOriginalTitle(), tmdbId);

    TmdbClient.MovieDetails enUs = tmdbClient.getMovieDetails(tmdbId, "en-US");
    TmdbClient.MovieDetails ru = tmdbClient.getMovieDetails(tmdbId, "ru");
    List<TmdbClient.AlternativeTitle> altTitles = tmdbClient.getAlternativeTitles(tmdbId);

    String titleEnUs = enUs != null ? enUs.title() : null;
    String titleRu = ru != null ? ru.title() : null;
    List<String> romanizedTitles = altTitles.stream()
        .filter(t -> "romanized title".equalsIgnoreCase(t.type()))
        .map(TmdbClient.AlternativeTitle::title)
        .toList();

    boolean updated = movie.enrichFromTmdb(
        titleEnUs,
        titleRu,
        romanizedTitles,
        enUs != null ? enUs.releaseDate() : null,
        enUs != null ? enUs.runtimeMinutes() : null,
        enUs != null ? enUs.imdbId() : null
    );

    movieRepository.save(movie);
    log.info("Enrichment done for '{}': updated={}", movie.getOriginalTitle(), updated);
  }
}
