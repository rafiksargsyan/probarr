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

  @GetMapping("/{id}")
  public ResponseEntity<EpisodeDTO> getEpisode(@PathVariable String tvShowId, @PathVariable String id) {
    return ResponseEntity.ok(episodeService.getEpisode(tvShowId, id));
  }

  @PostMapping
  public ResponseEntity<EpisodeDTO> createEpisode(@PathVariable String tvShowId,
                                                  @RequestBody EpisodeCreationDTO dto) {
    return new ResponseEntity<>(episodeService.createEpisode(tvShowId, dto), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<EpisodeDTO> updateEpisode(@PathVariable String tvShowId,
                                                  @PathVariable String id,
                                                  @RequestBody EpisodeCreationDTO dto) {
    return ResponseEntity.ok(episodeService.updateEpisode(tvShowId, id, dto));
  }

  @PostMapping("/{id}/scan")
  public ResponseEntity<EpisodeDTO> triggerScan(@PathVariable String tvShowId, @PathVariable String id) {
    return ResponseEntity.ok(episodeService.triggerScan(tvShowId, id));
  }

  @PostMapping("/{id}/blacklist/{infoHash}")
  public ResponseEntity<EpisodeDTO> addToBlackList(@PathVariable String tvShowId,
                                                   @PathVariable String id,
                                                   @PathVariable String infoHash) {
    return ResponseEntity.ok(episodeService.addToBlackList(tvShowId, id, infoHash));
  }

  @DeleteMapping("/{id}/blacklist/{infoHash}")
  public ResponseEntity<EpisodeDTO> removeFromBlackList(@PathVariable String tvShowId,
                                                        @PathVariable String id,
                                                        @PathVariable String infoHash) {
    return ResponseEntity.ok(episodeService.removeFromBlackList(tvShowId, id, infoHash));
  }

  @PostMapping("/{id}/whitelist/{infoHash}")
  public ResponseEntity<EpisodeDTO> addToWhiteList(@PathVariable String tvShowId,
                                                   @PathVariable String id,
                                                   @PathVariable String infoHash) {
    return ResponseEntity.ok(episodeService.addToWhiteList(tvShowId, id, infoHash));
  }

  @DeleteMapping("/{id}/whitelist/{infoHash}")
  public ResponseEntity<EpisodeDTO> removeFromWhiteList(@PathVariable String tvShowId,
                                                        @PathVariable String id,
                                                        @PathVariable String infoHash) {
    return ResponseEntity.ok(episodeService.removeFromWhiteList(tvShowId, id, infoHash));
  }

  @PostMapping("/{id}/cooldown/{infoHash}")
  public ResponseEntity<EpisodeDTO> addToCoolDown(@PathVariable String tvShowId,
                                                  @PathVariable String id,
                                                  @PathVariable String infoHash) {
    return ResponseEntity.ok(episodeService.addToCoolDown(tvShowId, id, infoHash));
  }

  @DeleteMapping("/{id}/cooldown/{infoHash}")
  public ResponseEntity<EpisodeDTO> removeFromCoolDown(@PathVariable String tvShowId,
                                                       @PathVariable String id,
                                                       @PathVariable String infoHash) {
    return ResponseEntity.ok(episodeService.removeFromCoolDown(tvShowId, id, infoHash));
  }
}
