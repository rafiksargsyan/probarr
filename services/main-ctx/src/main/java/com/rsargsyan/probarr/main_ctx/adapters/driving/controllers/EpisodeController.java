package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.EpisodeService;
import com.rsargsyan.probarr.main_ctx.core.app.dto.EpisodeCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.EpisodeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/tvshow/{tvShowId}/episode")
public class EpisodeController {

  private final EpisodeService episodeService;

  @Autowired
  public EpisodeController(EpisodeService episodeService) {
    this.episodeService = episodeService;
  }

  @GetMapping
  public ResponseEntity<List<EpisodeDTO>> listEpisodes(@PathVariable String tvShowId,
                                                       @RequestParam(required = false) Integer seasonNumber) {
    return ResponseEntity.ok(episodeService.listEpisodes(tvShowId, seasonNumber));
  }

  @PostMapping
  public ResponseEntity<EpisodeDTO> createEpisode(@PathVariable String tvShowId,
                                                  @RequestBody EpisodeCreationDTO dto) {
    return new ResponseEntity<>(episodeService.createEpisode(tvShowId, dto), HttpStatus.CREATED);
  }
}
