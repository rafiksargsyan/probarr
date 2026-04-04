package com.rsargsyan.probarr.main_ctx.core.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves episode numbers from a release title for a given season.
 *
 * <ul>
 *   <li>Returns {@code null} — season pack (add to all episodes in season)</li>
 *   <li>Returns empty list — no matching episode found (skip this release)</li>
 *   <li>Returns non-empty list — the specific episode numbers this release covers</li>
 * </ul>
 */
public final class EpisodeNumberResolver {

  private EpisodeNumberResolver() {}

  /**
   * @param title            release title
   * @param seasonNumber     the season we are scanning
   * @param showNames        all known names for the show (lowercase)
   * @param maxEpisodeNumber highest episode number in this season
   */
  public static List<Integer> resolve(String title, int seasonNumber,
                                      List<String> showNames, int maxEpisodeNumber) {
    // Strip resolution tags to avoid false positive number matches
    String titleLC = title.toLowerCase()
        .replaceAll("\\b(480p|720p|1080p|2160p)\\b", "");

    // Title must start with a known show name (dotted or spaced form)
    boolean prefixMatched = false;
    for (String name : showNames) {
      String dotted = name.replaceAll("\\s+", ".");
      if (titleLC.startsWith(name) || titleLC.startsWith(dotted)) {
        prefixMatched = true;
        break;
      }
    }
    if (!prefixMatched) {
      return List.of();
    }

    // Check season is present in title
    Pattern seasonRange = Pattern.compile(String.format("s([0-9]+)[-\\s]+([0-9]+)", ""));
    Pattern multiSeason = Pattern.compile("s([0-9]+).*s([0-9]+).*s([0-9]+)");
    Pattern seasonDirect = Pattern.compile(String.format("s0*%d[^\\d]", seasonNumber));
    Pattern seasonTemporada = Pattern.compile(String.format("temporada\\s*0*%d[^\\d]", seasonNumber));
    Pattern seasonSaison = Pattern.compile(String.format("saison\\s*0*%d[^\\d]", seasonNumber));

    Matcher rangeM = seasonRange.matcher(titleLC);
    if (rangeM.find() && !multiSeason.matcher(titleLC).find()) {
      int start = Integer.parseInt(rangeM.group(1));
      int end = Integer.parseInt(rangeM.group(2));
      if (seasonNumber < start || seasonNumber > end) {
        return List.of();
      }
      // Season is in range — treat as season pack for this season
    } else if (!seasonDirect.matcher(titleLC).find()
        && !seasonTemporada.matcher(titleLC).find()
        && !seasonSaison.matcher(titleLC).find()) {
      return List.of();
    }

    // Episode range: S01E01-08 or S01E01 16
    Pattern episodeRange = Pattern.compile(String.format("s0*%d\\s*e([0-9]+)[-\\s]+([0-9]+)", seasonNumber));
    Matcher rangeEp = episodeRange.matcher(titleLC);
    if (rangeEp.find()) {
      int startEp = Integer.parseInt(rangeEp.group(1));
      int endEp = Math.min(Integer.parseInt(rangeEp.group(2)), maxEpisodeNumber);
      if (startEp <= endEp) {
        List<Integer> result = new ArrayList<>();
        for (int i = startEp; i <= endEp; i++) result.add(i);
        return result;
      }
    }

    // Single episode: S01E03
    Pattern singleEp = Pattern.compile(String.format("s0*%d\\s*e([0-9]+)", seasonNumber));
    Matcher singleM = singleEp.matcher(titleLC);
    if (singleM.find()) {
      return List.of(Integer.parseInt(singleM.group(1)));
    }

    // No episode indicator found — it's a season pack
    return null;
  }
}
