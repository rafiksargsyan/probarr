package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.Util;
import com.rsargsyan.probarr.main_ctx.core.app.dto.MovieCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.MovieDTO;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.BlacklistReason;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.MovieRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MovieService {

  private final MovieRepository movieRepository;
  private final MovieScanTransactionService movieScanTransactionService;
  private final ApplicationEventPublisher eventPublisher;

  @Autowired
  public MovieService(MovieRepository movieRepository,
                      MovieScanTransactionService movieScanTransactionService,
                      ApplicationEventPublisher eventPublisher) {
    this.movieRepository = movieRepository;
    this.movieScanTransactionService = movieScanTransactionService;
    this.eventPublisher = eventPublisher;
  }

  public Page<MovieDTO> listMovies(Pageable pageable) {
    return movieRepository.findAll(pageable).map(MovieDTO::from);
  }

  public MovieDTO getMovie(String idStr) {
    Long id = Util.validateTSID(idStr);
    return MovieDTO.from(movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new));
  }

  @Transactional
  public MovieDTO createMovie(MovieCreationDTO dto) {
    Movie movie = new Movie(dto.originalTitle(), dto.originalLocale(), dto.releaseDate(),
        dto.runtimeMinutes(), dto.tmdbId(), dto.imdbId());
    if (dto.alternativeTitles() != null) {
      movie.setAlternativeTitles(dto.alternativeTitles());
    }
    movieRepository.save(movie);
    if (movie.getTmdbId() != null) {
      eventPublisher.publishEvent(new MovieTmdbIdAssignedEvent(movie.getId()));
    }
    return MovieDTO.from(movie);
  }

  @Transactional
  public MovieDTO updateMovie(String idStr, MovieCreationDTO dto) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    Long oldTmdbId = movie.getTmdbId();
    movie.update(dto.originalTitle(), dto.originalLocale(), dto.releaseDate(),
        dto.runtimeMinutes(), dto.tmdbId(), dto.imdbId());
    if (dto.alternativeTitles() != null) {
      movie.setAlternativeTitles(dto.alternativeTitles());
    }
    movieRepository.save(movie);
    if (movie.getTmdbId() != null && !movie.getTmdbId().equals(oldTmdbId)) {
      eventPublisher.publishEvent(new MovieTmdbIdAssignedEvent(movie.getId()));
    }
    return MovieDTO.from(movie);
  }

  @Transactional
  public MovieDTO addToBlackList(String idStr, String infoHash) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    movie.addToBlackList(infoHash, BlacklistReason.MANUAL);
    movieRepository.save(movie);
    return MovieDTO.from(movie);
  }

  @Transactional
  public MovieDTO removeFromBlackList(String idStr, String infoHash) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    movie.removeFromBlackList(infoHash);
    movieRepository.save(movie);
    return MovieDTO.from(movie);
  }

  @Transactional
  public MovieDTO addToWhiteList(String idStr, String infoHash) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    movie.addToWhiteList(infoHash);
    movieRepository.save(movie);
    return MovieDTO.from(movie);
  }

  @Transactional
  public MovieDTO removeFromWhiteList(String idStr, String infoHash) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    movie.removeFromWhiteList(infoHash);
    movieRepository.save(movie);
    return MovieDTO.from(movie);
  }

  @Transactional
  public MovieDTO addToCoolDown(String idStr, String infoHash) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    movie.addToCoolDown(infoHash);
    movieRepository.save(movie);
    return MovieDTO.from(movie);
  }

  @Transactional
  public MovieDTO removeFromCoolDown(String idStr, String infoHash) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    movie.removeFromCoolDown(infoHash);
    movieRepository.save(movie);
    return MovieDTO.from(movie);
  }

  public MovieDTO triggerScan(String idStr) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    if (!movie.getReleaseCandidates().isEmpty()) {
      log.info("triggerScan: skipping '{}' — {} candidate(s) already pending",
          movie.getOriginalTitle(), movie.getReleaseCandidates().size());
      return MovieDTO.from(movie);
    }
    movieScanTransactionService.markScanning(id);
    Thread.ofVirtual().start(() -> {
      try {
        movieScanTransactionService.scanMovie(id);
      } catch (Exception e) {
        log.error("Async scan failed for movie {}: {}", id, e.getMessage());
        movieScanTransactionService.markScanDone(id);
      }
    });
    return MovieDTO.from(movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new));
  }

  @Transactional
  public MovieDTO setForceScan(String idStr, boolean forceScan) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    movie.setForceScan(forceScan);
    movieRepository.save(movie);
    return MovieDTO.from(movie);
  }
}
