package com.rsargsyan.probarr.main_ctx.core.domain.localentity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubtitleTrack(
    int streamIndex,
    Locale language,
    SubsType subsType,
    SubsAuthor author
) {}
