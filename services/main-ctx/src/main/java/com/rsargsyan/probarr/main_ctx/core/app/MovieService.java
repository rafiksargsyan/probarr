package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.Util;
import com.rsargsyan.probarr.main_ctx.core.app.dto.MovieCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.MovieDTO;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.MovieRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

  private final MovieRepository movieRepository;

  @Autowired
  public MovieService(MovieRepository movieRepository) {
    this.movieRepository = movieRepository;
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
    return MovieDTO.from(movie);
  }

  @Transactional
  public MovieDTO updateMovie(String idStr, MovieCreationDTO dto) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    movie.update(dto.originalTitle(), dto.originalLocale(), dto.releaseDate(),
        dto.runtimeMinutes(), dto.tmdbId(), dto.imdbId());
    if (dto.alternativeTitles() != null) {
      movie.setAlternativeTitles(dto.alternativeTitles());
    }
    movieRepository.save(movie);
    return MovieDTO.from(movie);
  }

  @Transactional
  public MovieDTO addToBlackList(String idStr, String infoHash) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    movie.addToBlackList(infoHash);
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
  public MovieDTO setForceScan(String idStr, boolean forceScan) {
    Long id = Util.validateTSID(idStr);
    Movie movie = movieRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    movie.setForceScan(forceScan);
    movieRepository.save(movie);
    return MovieDTO.from(movie);
  }
}
