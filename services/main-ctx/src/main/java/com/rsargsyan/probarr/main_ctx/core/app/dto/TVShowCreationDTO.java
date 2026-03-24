package com.rsargsyan.probarr.main_ctx.core.app.dto;

public record TVShowCreationDTO(
    String originalTitle,
    String imdbId,
    Long tvdbId,
    Long sonarrId
) {}
