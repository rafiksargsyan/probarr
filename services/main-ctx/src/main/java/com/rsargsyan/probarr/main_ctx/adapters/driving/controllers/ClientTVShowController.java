package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

import com.rsargsyan.probarr.main_ctx.core.app.EpisodeService;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ClientEpisodeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tvshow")
public class ClientTVShowController {

  private final EpisodeService episodeService;

  @Autowired
  public ClientTVShowController(EpisodeService episodeService) {
    this.episodeService = episodeService;
  }

  @GetMapping("/{id}/season/{seasonNumber}/episode/{episodeNumber}")
  public ResponseEntity<ClientEpisodeDTO> getEpisodeByTvShowId(
      @PathVariable String id,
      @PathVariable int seasonNumber,
      @PathVariable int episodeNumber) {
    return ResponseEntity.ok(episodeService.getClientEpisodeByTvShowId(id, seasonNumber, episodeNumber));
  }

  @GetMapping("/by-tmdb/{tmdbId}/season/{seasonNumber}/episode/{episodeNumber}")
  public ResponseEntity<ClientEpisodeDTO> getEpisodeByTmdbId(
      @PathVariable Long tmdbId,
      @PathVariable int seasonNumber,
      @PathVariable int episodeNumber) {
    return ResponseEntity.ok(episodeService.getClientEpisodeByTvShowTmdbId(tmdbId, seasonNumber, episodeNumber));
  }

  @GetMapping("/by-tvdb/{tvdbId}/season/{seasonNumber}/episode/{episodeNumber}")
  public ResponseEntity<ClientEpisodeDTO> getEpisodeByTvdbId(
      @PathVariable Long tvdbId,
      @PathVariable int seasonNumber,
      @PathVariable int episodeNumber) {
    return ResponseEntity.ok(episodeService.getClientEpisodeByTvShowTvdbId(tvdbId, seasonNumber, episodeNumber));
  }
}
