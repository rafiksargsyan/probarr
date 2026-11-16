package com.rsargsyan.probarr.main_ctx.core.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anime-specific fork of {@link EpisodeNumberResolver}. Fansub/anime release titles have naming
 * conventions the plain resolver rejects or mis-parses, so this copy carries anime-only fixes
 * while {@link EpisodeNumberResolver} stays untouched for non-anime shows:
 * <ul>
 *   <li>tolerates a leading release-group tag ("[SubsPlease] Show ...") before the show name</li>
 *   <li>treats an immediately-following season/episode token ("S03E09") as a clean boundary,
 *       not a suspected sequel-title numeral</li>
 *   <li>recognizes an episode range where both ends carry an "E" prefix ("S03E09-E12"), not just
 *       a bare trailing number ("S03E09-12")</li>
 *   <li>recognizes an absolute episode range with no season marker at all ("(25-47)" - SubsPlease's
 *       standard batch naming), translating it to this season's relative numbers via the show's
 *       per-season episode offset</li>
 * </ul>
 *
 * <ul>
 *   <li>Returns {@code null} — season pack (add to all episodes in season)</li>
 *   <li>Returns empty list — no matching episode found (skip this release)</li>
 *   <li>Returns non-empty list — the specific episode numbers this release covers</li>
 * </ul>
 */
public final class AnimeEpisodeNumberResolver {

  private AnimeEpisodeNumberResolver() {}

  private static final Pattern YEAR = Pattern.compile("(19|20)\\d{2}");
  private static final Pattern SEASON_EPISODE_TOKEN = Pattern.compile("^s\\d{1,2}(e\\d{1,4})?$");
  private static final Pattern SEASON_WORD_TOKEN = Pattern.compile("^season\\d{0,2}$");
  private static final Pattern LEADING_TOKEN = Pattern.compile("^[\\s.:,']*([a-z0-9]+)");
  private static final Pattern LEADING_GROUP_TAGS = Pattern.compile("^(?:\\s*[\\[(][^\\])]*[\\])])+\\s*");
  private static final Pattern BARE_EPISODE_RANGE = Pattern.compile("\\be?([0-9]+)\\s*-\\s*e?([0-9]+)\\b");
  private static final Pattern BARE_EPISODE_SINGLE = Pattern.compile("\\be([0-9]+)\\b");

  /**
   * A prefix match against a show's name is only trustworthy if what immediately follows the
   * matched name is either nothing (a clean boundary into release metadata like "[", "(", "-"),
   * or the show's own expected metadata (a year, a season/episode marker like "S03E09", or the
   * word "season" - "Jujutsu Kaisen Season 3"). If any other word immediately follows, the title
   * is very likely for a *different, more specific* show whose name happens to share this one as
   * a prefix (e.g. "Planet Earth" vs "Planet Earth III") - reject the match in that case rather
   * than silently attributing a different show's episodes to this one.
   */
  private static boolean hasCleanBoundaryAfterPrefix(String rest) {
    Matcher m = LEADING_TOKEN.matcher(rest);
    if (!m.find()) return true; // next char is a real separator ('[', '(', '-', '/', ...) or end of title
    String token = m.group(1);
    return YEAR.matcher(token).matches()
        || SEASON_EPISODE_TOKEN.matcher(token).matches()
        || SEASON_WORD_TOKEN.matcher(token).matches();
  }

  /**
   * @param title            release title
   * @param seasonNumber     the season we are scanning
   * @param showNames        all known names for the show (lowercase)
   * @param maxEpisodeNumber highest episode number in this season
   * @param singleSeason     true if the show has exactly one season - allows recognizing a bare
   *                         episode range/number with no "S" marker at all (e.g. "(1-11 11)",
   *                         "E1-11"), since there's no other season it could ambiguously mean
   * @param absoluteOffset   total episode count of all seasons before this one - used to convert
   *                         an absolute episode range (no season marker at all) to this season's
   *                         relative numbers; pass 0 for season 1 / single-season shows
   */
  public static List<Integer> resolve(String title, int seasonNumber,
                                      List<String> showNames, int maxEpisodeNumber,
                                      boolean singleSeason, int absoluteOffset) {
    // Strip resolution tags to avoid false positive number matches
    String titleLC = title.toLowerCase()
        .replaceAll("\\b(480p|720p|1080p|2160p)\\b", "");

    // Fansub releases almost always lead with a release-group tag ("[SubsPlease] ...",
    // "[DubZoku] ..."), which breaks a literal startsWith check against the show name - strip
    // any leading bracket/paren tag group(s) before checking the prefix.
    String prefixCandidate = LEADING_GROUP_TAGS.matcher(titleLC).replaceFirst("");

    // Title must start with a known show name (dotted or spaced form), and whatever immediately
    // follows the matched name must not look like a different, more specific show's title
    // (e.g. a sequel numeral) - see hasCleanBoundaryAfterPrefix.
    boolean prefixMatched = false;
    for (String name : showNames) {
      String dotted = name.replaceAll("\\s+", ".");
      if (prefixCandidate.startsWith(name) && hasCleanBoundaryAfterPrefix(prefixCandidate.substring(name.length()))) {
        prefixMatched = true;
        break;
      }
      if (prefixCandidate.startsWith(dotted) && hasCleanBoundaryAfterPrefix(prefixCandidate.substring(dotted.length()))) {
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
    // Unlike the patterns above, these aren't scoped to the season being tested - used to confirm
    // the title doesn't commit to a season at all (any number), before treating a bare number
    // range as absolute (see below). Without this, a title that explicitly says "S01" would still
    // fall through to the absolute-range tier when testing season 2 (seasonDirect only checks for
    // "s02", not "is any season mentioned"), misreading an unrelated number pair as absolute
    // episodes.
    Pattern anySeasonDirect = Pattern.compile("\\bs[0-9]+[^\\d]");
    Pattern anySeasonWord = Pattern.compile("\\bseason\\s*[0-9]+[^\\d]");

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
      // No season marker for THIS season. For a multi-season show this is normally a reject -
      // unless the title carries a bare episode range AND doesn't commit to any season at all
      // (checked via anySeasonDirect/anySeasonWord, not just this season's), which fansub batch
      // releases (e.g. SubsPlease's "(25-47)") use to mean an ABSOLUTE episode range across the
      // whole series, not season-relative. Translate it via this season's absolute offset before
      // giving up. Guarded against a year range like "(2021-2026)" being misread as absolute
      // episodes.
      Matcher bareRangeM = BARE_EPISODE_RANGE.matcher(titleLC);
      if (bareRangeM.find()
          && !anySeasonDirect.matcher(titleLC).find()
          && !anySeasonWord.matcher(titleLC).find()) {
        int absStart = Integer.parseInt(bareRangeM.group(1));
        int absEnd = Integer.parseInt(bareRangeM.group(2));
        boolean looksLikeYearRange = absStart >= 1900 && absStart <= 2099
            && absEnd >= 1900 && absEnd <= 2099;
        if (!looksLikeYearRange && absStart <= absEnd) {
          int relStart = Math.max(absStart - absoluteOffset, 1);
          int relEnd = Math.min(absEnd - absoluteOffset, maxEpisodeNumber);
          if (relStart <= relEnd) {
            List<Integer> result = new ArrayList<>();
            for (int i = relStart; i <= relEnd; i++) result.add(i);
            return result;
          }
        }
      }
      return List.of();
    }

    // Episode range: S01E01-08 or S01E01-E08
    Pattern episodeRange = Pattern.compile(String.format("s0*%d\\s*e([0-9]+)\\s*-\\s*e?([0-9]+)", seasonNumber));
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
    // 12" (seen from groups like Greek-Nakama/Naruto-Kun.Hu). Only tried when the season wasn't
    // already explained by a genuine tight season-range match above (viaRange) - "S01-02" stays
    // a season-pack match for both seasons instead of getting reinterpreted as "season 1,
    // episode 2".
    if (!viaRange) {
      Pattern seasonDashEp = Pattern.compile(String.format("s0*%d\\s*-\\s*([0-9]+)(?!\\d)", seasonNumber));
      Matcher dashM = seasonDashEp.matcher(titleLC);
      if (dashM.find()) {
        return List.of(Integer.parseInt(dashM.group(1)));
      }
    }

    if (singleSeason) {
      // Bare episode range, no "S" marker: "(1-11 11)", "E1-11", "E1-E11"
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
