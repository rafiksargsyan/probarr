package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.localentity.AudioTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioVoiceType;

public record AudioTrackDTO(
    String language,
    String codec,
    Integer channels,
    boolean isDefault,
    AudioVoiceType voiceType,
    AudioAuthor author
) {
  public static AudioTrackDTO from(AudioTrack track) {
    return new AudioTrackDTO(
        track.language(),
        track.codec(),
        track.channels(),
        track.isDefault(),
        track.voiceType(),
        track.author()
    );
  }

  public AudioTrack toEntity() {
    return new AudioTrack(language, codec, channels, isDefault, voiceType, author);
  }
}
