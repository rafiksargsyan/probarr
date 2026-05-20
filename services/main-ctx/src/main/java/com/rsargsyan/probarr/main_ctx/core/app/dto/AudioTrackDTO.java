package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.localentity.AudioTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioVoiceType;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;

public record AudioTrackDTO(
    int streamIndex,
    Locale language,
    Integer channels,
    AudioVoiceType voiceType,
    AudioAuthor author
) {
  public static AudioTrackDTO from(AudioTrack track) {
    return new AudioTrackDTO(
        track.streamIndex(),
        track.language(),
        track.channels(),
        track.voiceType(),
        track.author()
    );
  }

  public AudioTrack toEntity() {
    return new AudioTrack(streamIndex, language, channels, voiceType, author);
  }
}
