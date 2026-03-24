package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.SeasonService;
import com.rsargsyan.probarr.main_ctx.core.app.dto.SeasonDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/tvshow/{tvShowId}/season")
public class SeasonController {

  private final SeasonService seasonService;

  @Autowired
  public SeasonController(SeasonService seasonService) {
    this.seasonService = seasonService;
  }

  @GetMapping
  public ResponseEntity<List<SeasonDTO>> listSeasons(@PathVariable String tvShowId) {
    return ResponseEntity.ok(seasonService.listSeasons(tvShowId));
  }

  @PostMapping
  public ResponseEntity<SeasonDTO> createSeason(@PathVariable String tvShowId,
                                                @RequestBody SeasonDTO dto) {
    return new ResponseEntity<>(seasonService.createSeason(tvShowId, dto), HttpStatus.CREATED);
  }
}
