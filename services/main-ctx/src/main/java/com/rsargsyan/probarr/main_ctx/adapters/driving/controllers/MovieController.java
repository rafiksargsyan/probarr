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

  @PostMapping("/{id}/scan")
  public ResponseEntity<MovieDTO> triggerScan(@PathVariable String id) {
    return ResponseEntity.ok(movieService.triggerScan(id));
  }

  @PatchMapping("/{id}/force-scan")
  public ResponseEntity<MovieDTO> setForceScan(@PathVariable String id,
                                               @RequestParam boolean value) {
    return ResponseEntity.ok(movieService.setForceScan(id, value));
  }

  @PostMapping("/{id}/blacklist/{infoHash}")
  public ResponseEntity<MovieDTO> addToBlackList(@PathVariable String id,
                                                 @PathVariable String infoHash) {
    return ResponseEntity.ok(movieService.addToBlackList(id, infoHash));
  }

  @DeleteMapping("/{id}/blacklist/{infoHash}")
  public ResponseEntity<MovieDTO> removeFromBlackList(@PathVariable String id,
                                                      @PathVariable String infoHash) {
    return ResponseEntity.ok(movieService.removeFromBlackList(id, infoHash));
  }

  @PostMapping("/{id}/whitelist/{infoHash}")
  public ResponseEntity<MovieDTO> addToWhiteList(@PathVariable String id,
                                                 @PathVariable String infoHash) {
    return ResponseEntity.ok(movieService.addToWhiteList(id, infoHash));
  }

  @DeleteMapping("/{id}/whitelist/{infoHash}")
  public ResponseEntity<MovieDTO> removeFromWhiteList(@PathVariable String id,
                                                      @PathVariable String infoHash) {
    return ResponseEntity.ok(movieService.removeFromWhiteList(id, infoHash));
  }

  @PostMapping("/{id}/cooldown/{infoHash}")
  public ResponseEntity<MovieDTO> addToCoolDown(@PathVariable String id,
                                                @PathVariable String infoHash) {
    return ResponseEntity.ok(movieService.addToCoolDown(id, infoHash));
  }

  @DeleteMapping("/{id}/cooldown/{infoHash}")
  public ResponseEntity<MovieDTO> removeFromCoolDown(@PathVariable String id,
                                                     @PathVariable String infoHash) {
    return ResponseEntity.ok(movieService.removeFromCoolDown(id, infoHash));
  }
}
