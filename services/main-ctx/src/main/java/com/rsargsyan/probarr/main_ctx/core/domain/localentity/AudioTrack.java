package com.rsargsyan.probarr.main_ctx.core.domain.localentity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioVoiceType;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AudioTrack(
    int streamIndex,
    Locale language,
    Integer channels,
    AudioVoiceType voiceType,
    AudioAuthor author
) {}
