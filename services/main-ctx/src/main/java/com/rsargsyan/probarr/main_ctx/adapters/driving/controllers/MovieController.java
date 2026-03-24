package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.MovieService;
import com.rsargsyan.probarr.main_ctx.core.app.dto.MovieCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.MovieDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/movie")
public class MovieController {

  private final MovieService movieService;

  @Autowired
  public MovieController(MovieService movieService) {
    this.movieService = movieService;
  }

  @GetMapping
  public ResponseEntity<Page<MovieDTO>> listMovies(@PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(movieService.listMovies(pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<MovieDTO> getMovie(@PathVariable String id) {
    return ResponseEntity.ok(movieService.getMovie(id));
  }

  @PostMapping
  public ResponseEntity<MovieDTO> createMovie(@RequestBody MovieCreationDTO dto) {
    return new ResponseEntity<>(movieService.createMovie(dto), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<MovieDTO> updateMovie(@PathVariable String id,
                                              @RequestBody MovieCreationDTO dto) {
    return ResponseEntity.ok(movieService.updateMovie(id, dto));
  }
}
