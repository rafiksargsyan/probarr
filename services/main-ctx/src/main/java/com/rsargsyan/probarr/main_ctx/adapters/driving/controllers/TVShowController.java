package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.TVShowService;
import com.rsargsyan.probarr.main_ctx.core.app.dto.TVShowCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.TVShowDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tvshow")
public class TVShowController {

  private final TVShowService tvShowService;

  @Autowired
  public TVShowController(TVShowService tvShowService) {
    this.tvShowService = tvShowService;
  }

  @GetMapping
  public ResponseEntity<Page<TVShowDTO>> listTVShows(@PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(tvShowService.listTVShows(pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<TVShowDTO> getTVShow(@PathVariable String id) {
    return ResponseEntity.ok(tvShowService.getTVShow(id));
  }

  @PostMapping
  public ResponseEntity<TVShowDTO> createTVShow(@RequestBody TVShowCreationDTO dto) {
    return new ResponseEntity<>(tvShowService.createTVShow(dto), HttpStatus.CREATED);
  }
}
