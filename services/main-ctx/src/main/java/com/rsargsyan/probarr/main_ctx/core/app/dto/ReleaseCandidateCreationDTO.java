package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.CandidateSource;

public record ReleaseCandidateCreationDTO(
    String movieId,    // optional TSID string
    String episodeId,  // optional TSID string
    String name,
    CandidateSource source,
    String tracker
) {}
