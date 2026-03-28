package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Edition;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Resolution;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.RipType;

import java.util.List;

public record ReleaseCreationDTO(
    String filePath,
    Long fileSizeBytes,
    String videoCodec,
    Resolution resolution,
    RipType ripType,
    Edition edition,
    Integer runtimeSeconds,
    List<AudioTrackDTO> audioTracks,
    List<SubtitleTrackDTO> subtitleTracks
) {}
