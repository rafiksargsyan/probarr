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

  private static final Pattern YEAR = Pattern.compile("(19|20)\\d{2}");
  private static final Pattern SEASON_WORD_TOKEN = Pattern.compile("^season\\d{0,2}$");
  private static final Pattern LEADING_TOKEN = Pattern.compile("^[\\s.:,']*([a-z0-9]+)");
  private static final Pattern BARE_EPISODE_RANGE = Pattern.compile("\\be?([0-9]+)\\s*-\\s*([0-9]+)\\b");
  private static final Pattern BARE_EPISODE_SINGLE = Pattern.compile("\\be([0-9]+)\\b");

  /**
   * A prefix match against a show's name is only trustworthy if what immediately follows the
   * matched name is either nothing (a clean boundary into release metadata like "[", "(", "-")
   * or the show's own expected metadata (a year, or the word "season" - e.g. "Jujutsu Kaisen
   * Season 3"). If a roman numeral, a small number, or any other word immediately follows, the
   * title is very likely for a *different, more specific* show whose name happens to share this
   * one as a prefix (e.g. "Planet Earth" vs "Planet Earth III", "Star Trek" vs "Star Trek:
   * Discovery") - reject the match in that case rather than silently attributing a different
   * show's episodes to this one.
   */
  private static boolean hasCleanBoundaryAfterPrefix(String rest) {
    Matcher m = LEADING_TOKEN.matcher(rest);
    if (!m.find()) return true; // next char is a real separator ('[', '(', '-', '/', ...) or end of title
    String token = m.group(1);
    return YEAR.matcher(token).matches() || SEASON_WORD_TOKEN.matcher(token).matches();
  }

  /**
   * @param title            release title
   * @param seasonNumber     the season we are scanning
   * @param showNames        all known names for the show (lowercase)
   * @param maxEpisodeNumber highest episode number in this season
   * @param singleSeason     true if the show has exactly one season - allows recognizing a bare
   *                         episode range/number with no "S" marker at all (e.g. "(1-11 11)",
   *                         "E1-11"), since there's no other season it could ambiguously mean
   */
  public static List<Integer> resolve(String title, int seasonNumber,
                                      List<String> showNames, int maxEpisodeNumber,
                                      boolean singleSeason) {
    // Strip resolution tags to avoid false positive number matches
    String titleLC = title.toLowerCase()
        .replaceAll("\\b(480p|720p|1080p|2160p)\\b", "");

    // Title must start with a known show name (dotted or spaced form), and whatever immediately
    // follows the matched name must not look like a different, more specific show's title
    // (e.g. a sequel numeral) - see hasCleanBoundaryAfterPrefix.
    boolean prefixMatched = false;
    for (String name : showNames) {
      String dotted = name.replaceAll("\\s+", ".");
      if (titleLC.startsWith(name) && hasCleanBoundaryAfterPrefix(titleLC.substring(name.length()))) {
        prefixMatched = true;
        break;
      }
      if (titleLC.startsWith(dotted) && hasCleanBoundaryAfterPrefix(titleLC.substring(dotted.length()))) {
        prefixMatched = true;
        break;
      }
    }
    if (!prefixMatched) {
      return List.of();
    }

    // Check season is present in title
    // Tight (no whitespace) on purpose: a genuine season range is written "S01-02" or "S01-S03".
    // Once whitespace is allowed around the dash it also matches "S3 - 12" and "S3 01-12", which
    // in practice mean "season 3, episode 12" / "season 3, episodes 1-12" - not a season range -
    // see the dash-episode tier below, which handles exactly that shape.
    Pattern seasonRange = Pattern.compile("s([0-9]+)-s?([0-9]+)");
    Pattern multiSeason = Pattern.compile("s([0-9]+).*s([0-9]+).*s([0-9]+)");
    Pattern seasonDirect = Pattern.compile(String.format("s0*%d[^\\d]", seasonNumber));
    Pattern seasonWord = Pattern.compile(String.format("season\\s*0*%d[^\\d]", seasonNumber));
    Pattern seasonTemporada = Pattern.compile(String.format("temporada\\s*0*%d[^\\d]", seasonNumber));
    Pattern seasonSaison = Pattern.compile(String.format("saison\\s*0*%d[^\\d]", seasonNumber));

    boolean viaRange = false;
    Matcher rangeM = seasonRange.matcher(titleLC);
    if (rangeM.find() && !multiSeason.matcher(titleLC).find()) {
      int start = Integer.parseInt(rangeM.group(1));
      int end = Integer.parseInt(rangeM.group(2));
      if (seasonNumber < start || seasonNumber > end) {
        return List.of();
      }
      viaRange = true;
      // Season is in range — treat as season pack for this season
    } else if (!seasonDirect.matcher(titleLC).find()
        && !seasonWord.matcher(titleLC).find()
        && !seasonTemporada.matcher(titleLC).find()
        && !seasonSaison.matcher(titleLC).find()
        && !singleSeason) {
      return List.of();
    }

    // Episode range: S01E01-08
    Pattern episodeRange = Pattern.compile(String.format("s0*%d\\s*e([0-9]+)\\s*-\\s*([0-9]+)", seasonNumber));
    Matcher rangeEp = episodeRange.matcher(titleLC);
    if (rangeEp.find()) {
      int startEp = Integer.parseInt(rangeEp.group(1));
      int rawEndEp = Integer.parseInt(rangeEp.group(2));
      if (startEp <= rawEndEp && rawEndEp <= maxEpisodeNumber) {
        List<Integer> result = new ArrayList<>();
        for (int i = startEp; i <= rawEndEp; i++) result.add(i);
        return result;
      }
    }

    // Single episode: S01E03
    Pattern singleEp = Pattern.compile(String.format("s0*%d\\s*e([0-9]+)", seasonNumber));
    Matcher singleM = singleEp.matcher(titleLC);
    if (singleM.find()) {
      return List.of(Integer.parseInt(singleM.group(1)));
    }

    // Season + bare dash + number, no "E" marker at all: "S3 - 12" meaning "season 3, episode
    // 12". Only tried when the season wasn't already explained by a genuine tight season-range
    // match above (viaRange) - "S01-02" stays a season-pack match for both seasons instead of
    // getting reinterpreted as "season 1, episode 2".
    if (!viaRange) {
      Pattern seasonDashEp = Pattern.compile(String.format("s0*%d\\s*-\\s*([0-9]+)(?!\\d)", seasonNumber));
      Matcher dashM = seasonDashEp.matcher(titleLC);
      if (dashM.find()) {
        return List.of(Integer.parseInt(dashM.group(1)));
      }
    }

    if (singleSeason) {
      // Bare episode range, no "S" marker: "(1-11 11)", "E1-11"
      Matcher bareRangeM = BARE_EPISODE_RANGE.matcher(titleLC);
      if (bareRangeM.find()) {
        int startEp = Integer.parseInt(bareRangeM.group(1));
        int rawEndEp = Integer.parseInt(bareRangeM.group(2));
        if (startEp >= 1 && startEp <= rawEndEp && rawEndEp <= maxEpisodeNumber) {
          List<Integer> result = new ArrayList<>();
          for (int i = startEp; i <= rawEndEp; i++) result.add(i);
          return result;
        }
      }
      // Bare single episode, no "S" marker: "E03"
      Matcher bareSingleM = BARE_EPISODE_SINGLE.matcher(titleLC);
      if (bareSingleM.find()) {
        int epNum = Integer.parseInt(bareSingleM.group(1));
        if (epNum >= 1 && epNum <= maxEpisodeNumber) {
          return List.of(epNum);
        }
      }
    }

    // No episode indicator found — it's a season pack
    return null;
  }
}
