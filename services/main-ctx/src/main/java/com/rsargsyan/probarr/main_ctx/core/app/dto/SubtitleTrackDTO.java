package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.localentity.SubtitleTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsType;

public record SubtitleTrackDTO(
    int streamIndex,
    Locale language,
    SubsType subsType,
    SubsAuthor author
) {
  public static SubtitleTrackDTO from(SubtitleTrack track) {
    return new SubtitleTrackDTO(
        track.streamIndex(),
        track.language(),
        track.subsType(),
        track.author()
    );
  }

  public SubtitleTrack toEntity() {
    return new SubtitleTrack(streamIndex, language, subsType, author);
  }
}
