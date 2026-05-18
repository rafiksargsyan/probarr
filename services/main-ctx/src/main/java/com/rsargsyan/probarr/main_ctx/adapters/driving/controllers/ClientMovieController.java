package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.MovieService;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ClientMovieDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movie")
public class ClientMovieController {

  private final MovieService movieService;

  @Autowired
  public ClientMovieController(MovieService movieService) {
    this.movieService = movieService;
  }

  @GetMapping
  public ResponseEntity<Page<ClientMovieDTO>> listMovies(@PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(movieService.listClientMovies(pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ClientMovieDTO> getMovie(@PathVariable String id) {
    return ResponseEntity.ok(movieService.getClientMovie(id));
  }

  @GetMapping("/by-tmdb/{tmdbId}")
  public ResponseEntity<ClientMovieDTO> getMovieByTmdbId(@PathVariable Long tmdbId) {
    return ResponseEntity.ok(movieService.getClientMovieByTmdbId(tmdbId));
  }

  @GetMapping("/torrent/{infoHash}")
  public ResponseEntity<byte[]> getTorrentBytes(@PathVariable String infoHash) {
    byte[] bytes = movieService.getTorrentBytes(infoHash);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment; filename=\"" + infoHash + ".torrent\"")
        .body(bytes);
  }
}
