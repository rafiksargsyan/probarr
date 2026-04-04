package com.rsargsyan.probarr.main_ctx.core.domain.localentity;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioVoiceType;

public record AudioTrack(
    String language,   // BCP-47, optional
    String codec,      // e.g. "aac", "ac3", "dts"
    Integer channels,  // e.g. 2, 6, 8
    boolean isDefault,
    AudioVoiceType voiceType,
    AudioAuthor author
) {}
