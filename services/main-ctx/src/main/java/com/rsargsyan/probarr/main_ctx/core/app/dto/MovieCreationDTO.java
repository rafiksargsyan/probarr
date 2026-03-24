package com.rsargsyan.probarr.main_ctx.core.app.dto;

public record MovieCreationDTO(
    String originalTitle,
    Integer year,
    String imdbId,
    Long tmdbId,
    Long radarrId
) {}
