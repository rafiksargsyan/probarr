package com.rsargsyan.probarr.main_ctx.core.domain.localentity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class SubtitleTrack {

  private String language;

  private String format; // e.g. "srt", "ass", "pgs"

  private boolean isDefault;

  private boolean isForced;

  public SubtitleTrack(String language, String format, boolean isDefault, boolean isForced) {
    this.language = language;
    this.format = format;
    this.isDefault = isDefault;
    this.isForced = isForced;
  }
}
