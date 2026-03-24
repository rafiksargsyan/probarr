package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.exception.InvalidEpisodeException;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

@Entity
public class Episode extends AggregateRoot {

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tvshow_id", nullable = false)
  private TVShow tvShow;

  @Getter
  @Column(name = "season_number")
  private Integer seasonNumber; // null for absolute-numbered episodes

  @Getter
  @Column(name = "episode_number")
  private Integer episodeNumber; // meaningful only when seasonNumber is present

  @Getter
  @Column(name = "absolute_number")
  private Integer absoluteNumber; // null for season-grouped episodes

  @Getter
  private LocalDate airDate;

  @Getter
  private Integer runtime; // minutes

  @SuppressWarnings("unused")
  Episode() {}

  public Episode(TVShow tvShow, Integer seasonNumber, Integer episodeNumber,
                 Integer absoluteNumber, LocalDate airDate, Integer runtime) {
    validate(seasonNumber, episodeNumber, absoluteNumber);
    this.tvShow = tvShow;
    this.seasonNumber = seasonNumber;
    this.episodeNumber = episodeNumber;
    this.absoluteNumber = absoluteNumber;
    this.airDate = airDate;
    this.runtime = runtime;
  }

  public void update(Integer seasonNumber, Integer episodeNumber, Integer absoluteNumber,
                     LocalDate airDate, Integer runtime) {
    validate(seasonNumber, episodeNumber, absoluteNumber);
    this.seasonNumber = seasonNumber;
    this.episodeNumber = episodeNumber;
    this.absoluteNumber = absoluteNumber;
    this.airDate = airDate;
    this.runtime = runtime;
    touch();
  }

  private static void validate(Integer seasonNumber, Integer episodeNumber, Integer absoluteNumber) {
    boolean hasSeasonEpisode = seasonNumber != null && episodeNumber != null;
    boolean hasAbsolute = absoluteNumber != null;
    if (!hasSeasonEpisode && !hasAbsolute) {
      throw new InvalidEpisodeException(
          "Episode must have either (seasonNumber + episodeNumber) or absoluteNumber");
    }
  }
}
