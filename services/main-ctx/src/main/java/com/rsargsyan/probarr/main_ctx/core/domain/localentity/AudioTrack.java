package com.rsargsyan.probarr.main_ctx.core.domain.localentity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class AudioTrack {

  private String language; // BCP-47, optional

  private String codec; // e.g. "aac", "ac3", "dts"

  private Integer channels; // e.g. 2, 6, 8

  private boolean isDefault;

  public AudioTrack(String language, String codec, Integer channels, boolean isDefault) {
    this.language = language;
    this.codec = codec;
    this.channels = channels;
    this.isDefault = isDefault;
  }
}
