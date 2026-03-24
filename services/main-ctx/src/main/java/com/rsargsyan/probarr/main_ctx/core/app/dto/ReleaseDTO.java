package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Release;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Resolution;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.RipType;

import java.time.Instant;
import java.util.List;

public record ReleaseDTO(
    String id,
    String candidateId,
    String filePath,
    Long fileSizeBytes,
    String videoCodec,
    Resolution resolution,
    RipType ripType,
    Integer runtimeSeconds,
    List<AudioTrackDTO> audioTracks,
    List<SubtitleTrackDTO> subtitleTracks,
    Instant createdAt
) {
  public static ReleaseDTO from(Release release) {
    return new ReleaseDTO(
        release.getStrId(),
        release.getCandidate().getStrId(),
        release.getFilePath(),
        release.getFileSizeBytes(),
        release.getVideoCodec(),
        release.getResolution(),
        release.getRipType(),
        release.getRuntimeSeconds(),
        release.getAudioTracks().stream().map(AudioTrackDTO::from).toList(),
        release.getSubtitleTracks().stream().map(SubtitleTrackDTO::from).toList(),
        release.getCreatedAt()
    );
  }
}
