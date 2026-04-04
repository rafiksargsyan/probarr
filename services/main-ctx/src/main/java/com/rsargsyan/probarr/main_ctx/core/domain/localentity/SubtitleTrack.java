package com.rsargsyan.probarr.main_ctx.core.domain.localentity;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsType;

public record SubtitleTrack(
    String language,
    String format,     // e.g. "srt", "ass", "pgs"
    boolean isDefault,
    boolean isForced,
    SubsType subsType,
    SubsAuthor author
) {}
