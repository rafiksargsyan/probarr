package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Season;

import java.time.LocalDate;

public record SeasonDTO(
    String id,
    String tvShowId,
    Integer seasonNumber,
    String originalName,
    LocalDate airDate
) {
  public static SeasonDTO from(Season season) {
    return new SeasonDTO(
        season.getStrId(),
        season.getTvShow().getStrId(),
        season.getSeasonNumber(),
        season.getOriginalName(),
        season.getAirDate()
    );
  }
}
