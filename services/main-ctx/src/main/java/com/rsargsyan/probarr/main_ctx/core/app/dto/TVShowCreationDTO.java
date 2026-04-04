package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;

import java.time.LocalDate;

public record TVShowCreationDTO(
    String originalTitle,
    Locale originalLocale,
    Long tmdbId,
    String imdbId,
    Long tvdbId,
    LocalDate releaseDate,
    boolean useTvdb
) {}
