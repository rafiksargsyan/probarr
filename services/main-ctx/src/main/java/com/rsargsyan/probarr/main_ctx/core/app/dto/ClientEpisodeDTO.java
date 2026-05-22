package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Episode;

import java.util.List;

public record ClientEpisodeDTO(
    String id,
    Long showTmdbId,
    int seasonNumber,
    int episodeNumber,
    List<ClientMovieDTO.ClientReleaseDTO> releases
) {
  public static ClientEpisodeDTO from(Episode episode) {
    return new ClientEpisodeDTO(
        episode.getStrId(),
        episode.getTvShow().getTmdbId(),
        episode.getSeasonNumber() != null ? episode.getSeasonNumber() : 0,
        episode.getEpisodeNumber() != null ? episode.getEpisodeNumber() : 0,
        episode.getReleases().stream().map(ClientMovieDTO.ClientReleaseDTO::from).toList()
    );
  }
}
