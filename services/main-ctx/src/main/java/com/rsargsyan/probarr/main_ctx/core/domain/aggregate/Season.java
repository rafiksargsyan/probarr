package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.exception.InvalidSeasonException;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

@Entity
@Table(
    name = "season",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tvshow_id", "season_number"})
)
public class Season extends AggregateRoot {

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tvshow_id", nullable = false)
  private TVShow tvShow;

  @Getter
  @Column(name = "season_number", nullable = false)
  private Integer seasonNumber;

  @Getter
  private String originalName; // nullable, e.g. "Murder House" for AHS

  @Getter
  private LocalDate airDate;

  @SuppressWarnings("unused")
  Season() {}

  public Season(TVShow tvShow, Integer seasonNumber, String originalName, LocalDate airDate) {
    if (seasonNumber == null || seasonNumber < 1) {
      throw new InvalidSeasonException("Season number must be a positive integer");
    }
    this.tvShow = tvShow;
    this.seasonNumber = seasonNumber;
    this.originalName = originalName;
    this.airDate = airDate;
  }

  public void update(String originalName, LocalDate airDate) {
    this.originalName = originalName;
    this.airDate = airDate;
    touch();
  }
}
