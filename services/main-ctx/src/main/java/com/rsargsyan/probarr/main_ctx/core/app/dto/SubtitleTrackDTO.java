package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.localentity.SubtitleTrack;

public record SubtitleTrackDTO(
    String language,
    String format,
    boolean isDefault,
    boolean isForced
) {
  public static SubtitleTrackDTO from(SubtitleTrack track) {
    return new SubtitleTrackDTO(
        track.getLanguage(),
        track.getFormat(),
        track.isDefault(),
        track.isForced()
    );
  }

  public SubtitleTrack toEntity() {
    return new SubtitleTrack(language, format, isDefault, isForced);
  }
}
