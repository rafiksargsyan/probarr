package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.localentity.AudioTrack;

public record AudioTrackDTO(
    String language,
    String codec,
    Integer channels,
    boolean isDefault
) {
  public static AudioTrackDTO from(AudioTrack track) {
    return new AudioTrackDTO(
        track.getLanguage(),
        track.getCodec(),
        track.getChannels(),
        track.isDefault()
    );
  }

  public AudioTrack toEntity() {
    return new AudioTrack(language, codec, channels, isDefault);
  }
}
