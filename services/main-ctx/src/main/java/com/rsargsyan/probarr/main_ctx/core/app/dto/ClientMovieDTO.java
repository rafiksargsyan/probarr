package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Movie;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.AudioTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.SubtitleTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Release;

import java.time.LocalDate;
import java.util.List;

public record ClientMovieDTO(
    String id,
    String originalTitle,
    LocalDate releaseDate,
    Integer runtimeMinutes,
    Long tmdbId,
    String imdbId,
    List<ClientReleaseDTO> releases
) {

  public static ClientMovieDTO from(Movie movie) {
    List<ClientReleaseDTO> releases = movie.getReleases().stream()
        .map(ClientReleaseDTO::from)
        .toList();
    return new ClientMovieDTO(
        movie.getStrId(),
        movie.getOriginalTitle(),
        movie.getReleaseDate(),
        movie.getRuntimeMinutes(),
        movie.getTmdbId(),
        movie.getImdbId(),
        releases
    );
  }

  public record ClientReleaseDTO(
      String infoHash,
      String resolution,
      String ripType,
      String torrentSource,
      Integer fileIndex,
      String magnetUri,
      boolean hasTorrentFile,
      List<ClientAudioTrackDTO> audioTracks,
      List<ClientSubtitleTrackDTO> subtitleTracks
  ) {
    public static ClientReleaseDTO from(Release r) {
      String src = r.torrentSource();
      boolean isMagnet = src != null && src.startsWith("magnet:");
      return new ClientReleaseDTO(
          r.infoHash(),
          r.resolution() != null ? r.resolution().name() : null,
          r.ripType() != null ? r.ripType().name() : null,
          src,
          r.fileIndex(),
          isMagnet ? src : null,
          !isMagnet && src != null,
          r.audioTracks().stream().map(ClientAudioTrackDTO::from).toList(),
          r.subtitleTracks().stream().map(ClientSubtitleTrackDTO::from).toList()
      );
    }
  }

  public record ClientAudioTrackDTO(String language, Integer channels) {
    public static ClientAudioTrackDTO from(AudioTrack t) {
      return new ClientAudioTrackDTO(t.language(), t.channels());
    }
  }

  public record ClientSubtitleTrackDTO(String language) {
    public static ClientSubtitleTrackDTO from(SubtitleTrack t) {
      return new ClientSubtitleTrackDTO(t.language());
    }
  }
}
