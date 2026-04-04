package com.rsargsyan.probarr.main_ctx.core.ports.client;

import java.time.LocalDate;
import java.util.List;

public interface TvdbClient {

  TvShow getTvShowById(Long tvdbId);

  String getTvShowTranslation(Long tvdbId, String lang);

  List<TvdbEpisode> getTvShowEpisodes(Long tvdbId);

  record TvShow(Long id, String name, List<String> nameTranslations) {}

  record TvdbEpisode(Long id, Integer seasonNumber, Integer episodeNumber,
                     Integer runtimeMinutes, LocalDate aired) {}
}
