package com.rsargsyan.probarr.main_ctx.core.app.dto;

import java.time.LocalDate;

public record EpisodeCreationDTO(
    Integer seasonNumber,
    Integer episodeNumber,
    Integer absoluteNumber,
    LocalDate airDate,
    Integer runtime
) {}
