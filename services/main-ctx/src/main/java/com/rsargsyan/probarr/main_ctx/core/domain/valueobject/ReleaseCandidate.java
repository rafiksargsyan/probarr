package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

import java.time.Instant;
import java.util.List;

public record ReleaseCandidate(
    String infoHash,
    String downloadUrl,
    String infoUrl,
    TorrentTracker tracker,
    Long sizeInBytes,
    Integer seeders,
    Resolution resolution,
    RipType ripType,
    Instant releaseAt,
    List<String> radarrLanguages,
    boolean radarrIsUnknown
) {}
