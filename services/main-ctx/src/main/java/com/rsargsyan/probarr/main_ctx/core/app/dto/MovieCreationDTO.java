package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;

import java.time.LocalDate;
import java.util.List;

public record MovieCreationDTO(
    String originalTitle,
    Locale originalLocale,
    LocalDate releaseDate,
    Integer runtimeMinutes,
    Long tmdbId,
    String imdbId,
    List<String> alternativeTitles
) {}
