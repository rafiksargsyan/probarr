package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Episode;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Season;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.TVShow;
import com.rsargsyan.probarr.main_ctx.core.ports.client.TmdbClient;
import com.rsargsyan.probarr.main_ctx.core.ports.client.TvdbClient;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.EpisodeRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.SeasonRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.TVShowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TVShowEnrichmentTransactionService {

  private static final String[] TMDB_LANGUAGES = {"en-US", "ru", "es", "fr", "it"};
  private static final List<String> TVDB_NAME_LANGUAGES = List.of("eng", "rus", "spa", "fra", "ita");

  private final TVShowRepository tvShowRepository;
  private final SeasonRepository seasonRepository;
  private final EpisodeRepository episodeRepository;
  private final TmdbClient tmdbClient;
  private final TvdbClient tvdbClient;

  @Autowired
  public TVShowEnrichmentTransactionService(TVShowRepository tvShowRepository,
                                             SeasonRepository seasonRepository,
                                             EpisodeRepository episodeRepository,
                                             TmdbClient tmdbClient,
                                             TvdbClient tvdbClient) {
    this.tvShowRepository = tvShowRepository;
    this.seasonRepository = seasonRepository;
    this.episodeRepository = episodeRepository;
    this.tmdbClient = tmdbClient;
    this.tvdbClient = tvdbClient;
  }

  @Transactional
  public void enrichTvShow(Long tvShowId) {
    TVShow tvShow = tvShowRepository.findById(tvShowId)
        .orElseThrow(() -> new IllegalArgumentException("TVShow not found: " + tvShowId));

    Long tmdbId = tvShow.getTmdbId();
    if (tmdbId == null) return;

    log.info("Enriching TV show '{}' tmdbId={} useTvdb={}", tvShow.getOriginalTitle(), tmdbId, tvShow.isUseTvdb());

    // Fetch external IDs from TMDB — always fetch imdbId, only fetch tvdbId if not already set
    TmdbClient.TvShowExternalIds externalIds = tmdbClient.getTvShowExternalIds(tmdbId);
    Long newTvdbId = (tvShow.getTvdbId() == null && externalIds != null) ? externalIds.tvdbId() : null;
    String newImdbId = externalIds != null ? externalIds.imdbId() : null;

    // Collect names and season numbers from TMDB in multiple languages
    java.time.LocalDate firstAirDate = null;
    List<Integer> tmdbSeasonNumbers = List.of();
    for (String lang : TMDB_LANGUAGES) {
      TmdbClient.TvShowDetails details = tmdbClient.getTvShowDetails(tmdbId, lang);
      if (details == null) continue;
      tvShow.addName(details.name());
      if (firstAirDate == null) firstAirDate = details.firstAirDate();
      if (tmdbSeasonNumbers.isEmpty() && details.seasonNumbers() != null) {
        tmdbSeasonNumbers = details.seasonNumbers();
      }
    }

    Long effectiveTvdbId = newTvdbId != null ? newTvdbId : tvShow.getTvdbId();

    // Add names from TVDB translations if we have a TVDB ID
    if (effectiveTvdbId != null) {
      addNamesFromTvdb(tvShow, effectiveTvdbId);
    }

    tvShow.enrichFromTmdb(firstAirDate, newImdbId, newTvdbId);

    if (!tvShow.isUseTvdb()) {
      syncSeasonsAndEpisodesFromTmdb(tvShow, tmdbId, tmdbSeasonNumbers);
    } else if (effectiveTvdbId != null) {
      syncSeasonsAndEpisodesFromTvdb(tvShow, effectiveTvdbId);
    } else {
      log.warn("TV show '{}' has useTvdb=true but no TVDB ID — skipping season/episode sync",
          tvShow.getOriginalTitle());
    }

    tvShowRepository.save(tvShow);
    log.info("Enrichment done for '{}'", tvShow.getOriginalTitle());
  }

  private void addNamesFromTvdb(TVShow tvShow, Long tvdbId) {
    try {
      TvdbClient.TvShow tvdbShow = tvdbClient.getTvShowById(tvdbId);
      if (tvdbShow == null) return;
      tvShow.addName(tvdbShow.name());
      for (String lang : TVDB_NAME_LANGUAGES) {
        if (tvdbShow.nameTranslations().contains(lang)) {
          String translatedName = tvdbClient.getTvShowTranslation(tvdbId, lang);
          tvShow.addName(translatedName);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to fetch TVDB names for tvdbId={}: {}", tvdbId, e.getMessage());
    }
  }

  private void syncSeasonsAndEpisodesFromTmdb(TVShow tvShow, Long tmdbId, List<Integer> seasonNumbers) {
    for (Integer seasonNumber : seasonNumbers) {
      Season season = getOrCreateSeason(tvShow, seasonNumber);
      if (season.getTmdbSeasonNumber() == null) {
        season.setTmdbSeasonNumber(seasonNumber);
        seasonRepository.save(season);
      }

      List<TmdbClient.SeasonEpisodeDetails> episodes = tmdbClient.getSeasonEpisodes(tmdbId, seasonNumber);
      for (TmdbClient.SeasonEpisodeDetails ep : episodes) {
        if (ep.episodeNumber() <= 0) continue;
        syncEpisode(tvShow, seasonNumber, ep.episodeNumber(), ep.runtimeMinutes(), ep.airDate());
      }
    }
  }

  private void syncSeasonsAndEpisodesFromTvdb(TVShow tvShow, Long tvdbId) {
    List<TvdbClient.TvdbEpisode> episodes;
    try {
      episodes = tvdbClient.getTvShowEpisodes(tvdbId);
    } catch (Exception e) {
      log.error("Failed to fetch TVDB episodes for tvdbId={}: {}", tvdbId, e.getMessage());
      return;
    }

    for (TvdbClient.TvdbEpisode ep : episodes) {
      if (ep.seasonNumber() == null || ep.seasonNumber() <= 0) continue;
      if (ep.episodeNumber() == null || ep.episodeNumber() <= 0) continue;

      Season season = getOrCreateSeason(tvShow, ep.seasonNumber());
      if (season.getTvdbSeasonNumber() == null) {
        season.setTvdbSeasonNumber(ep.seasonNumber());
        seasonRepository.save(season);
      }

      syncEpisode(tvShow, ep.seasonNumber(), ep.episodeNumber(), ep.runtimeMinutes(), ep.aired());
    }
  }

  private Season getOrCreateSeason(TVShow tvShow, int seasonNumber) {
    return seasonRepository.findByTvShowIdAndSeasonNumber(tvShow.getId(), seasonNumber)
        .orElseGet(() -> {
          Season s = new Season(tvShow, seasonNumber, null, null);
          seasonRepository.save(s);
          log.info("Created season {} for '{}'", seasonNumber, tvShow.getOriginalTitle());
          return s;
        });
  }

  private void syncEpisode(TVShow tvShow, int seasonNumber, int episodeNumber,
                            Integer runtimeMinutes, java.time.LocalDate airDate) {
    episodeRepository.findByTvShowIdAndSeasonNumberAndEpisodeNumber(
            tvShow.getId(), seasonNumber, episodeNumber)
        .ifPresentOrElse(
            existing -> {
              boolean changed = false;
              Integer newRuntime = runtimeMinutes != null ? runtimeMinutes * 60 : null;
              if (existing.getRuntimeSeconds() == null && newRuntime != null) changed = true;
              if (existing.getAirDate() == null && airDate != null) changed = true;
              if (changed) {
                existing.update(existing.getSeasonNumber(), existing.getEpisodeNumber(),
                    existing.getAbsoluteNumber(),
                    existing.getAirDate() != null ? existing.getAirDate() : airDate,
                    existing.getRuntimeSeconds() != null ? existing.getRuntimeSeconds() : newRuntime);
                episodeRepository.save(existing);
              }
            },
            () -> {
              Integer runtimeSeconds = runtimeMinutes != null ? runtimeMinutes * 60 : null;
              Episode ep = new Episode(tvShow, seasonNumber, episodeNumber, null, airDate, runtimeSeconds);
              episodeRepository.save(ep);
              log.debug("Created S{}E{} for '{}'", seasonNumber, episodeNumber, tvShow.getOriginalTitle());
            }
        );
  }
}
