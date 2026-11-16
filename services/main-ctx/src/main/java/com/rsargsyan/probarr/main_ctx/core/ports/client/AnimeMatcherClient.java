package com.rsargsyan.probarr.main_ctx.core.ports.client;

import java.util.List;

/**
 * LLM-backed matching for anime, where fansub release-title conventions (leading group tag,
 * absolute episode numbering across seasons, batch ranges) make regex matching brittle. Used in
 * place of {@link com.rsargsyan.probarr.main_ctx.core.domain.service.EpisodeNumberResolver} and
 * the regex fallbacks in episode-file matching, only for shows flagged as anime.
 */
public interface AnimeMatcherClient {

  record SeasonSummary(int seasonNumber, int episodeCount) {}

  record SeasonMatch(int seasonNumber, boolean fullSeason, List<Integer> episodeNumbers) {}

  /**
   * @return the season(s)/episode(s) this release title covers, or an empty list if it doesn't
   *         match this show at all.
   */
  List<SeasonMatch> matchRelease(String releaseTitle, List<String> showNames, List<SeasonSummary> seasons);

  record FileCandidate(int index, String name, long sizeBytes) {}

  /**
   * @return the file index that is this exact episode, or null if none of the files confidently match.
   */
  Integer locateEpisodeFile(List<FileCandidate> files, List<String> showNames,
                            Integer seasonNumber, Integer episodeNumber, Integer absoluteNumber);
}
