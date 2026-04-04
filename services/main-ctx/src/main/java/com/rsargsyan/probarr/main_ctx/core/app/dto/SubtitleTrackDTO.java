package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.localentity.SubtitleTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsType;

public record SubtitleTrackDTO(
    String language,
    String format,
    boolean isDefault,
    boolean isForced,
    SubsType subsType,
    SubsAuthor author
) {
  public static SubtitleTrackDTO from(SubtitleTrack track) {
    return new SubtitleTrackDTO(
        track.language(),
        track.format(),
        track.isDefault(),
        track.isForced(),
        track.subsType(),
        track.author()
    );
  }

  public SubtitleTrack toEntity() {
    return new SubtitleTrack(language, format, isDefault, isForced, subsType, author);
  }
}
