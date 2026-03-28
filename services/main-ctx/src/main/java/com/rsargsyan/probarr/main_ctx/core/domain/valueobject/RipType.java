package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

import java.util.regex.Pattern;

public enum RipType {
  CAM,
  TELESYNC,
  DVD,
  HDTV,
  WEB,
  BR;

  // Mirrors Radarr's SourceRegex groups, ordered most-specific first
  private static final Pattern BR_REGEX = Pattern.compile(
      "\\b(M?Blu[-_. ]?Ray|HD[-_. ]?DVD|UHD2?BD|BDISO|BDMux|BD25|BD50|BDRip|BDLight|UHDBDRip|BRRip)\\b",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern WEBDL_REGEX = Pattern.compile(
      "\\bWEB[-_. ]?DL(?:mux)?\\b"
      + "|\\b(AmazonHD|AmazonSD|iTunesHD|NetflixU?HD|WebHD|HBOMaxHD|DisneyHD|MaxdomeHD)\\b"
      + "|[. ]WEB[. ](?:[xh][ .]?26[45]|AVC|HEVC|DDP?5[. ]1)"
      + "|\\b(?:AMZN|NF|DP)[. -]WEB[. -](?!Rip)",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern WEBRIP_REGEX = Pattern.compile(
      "\\b(WebRip|Web-Rip|WEBMux)\\b",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern HDTV_REGEX = Pattern.compile(
      "\\b(HDTV|PDTV|SDTV|TVRip|HD[-_. ]TV|SD[-_. ]TV)\\b"
      + "|\\b(WS[-_. ])?DSR\\b",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern DVD_REGEX = Pattern.compile(
      "\\b(DVDRip|DVDSCR|DVDSCREENER|xvidvd)\\b"
      + "|\\bDVD(?!-R)\\b"
      + "|\\b\\d?x?M?DVD-?[R59]\\b",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern TELESYNC_REGEX = Pattern.compile(
      "\\b(TELESYNCH?|HD-TS|HDTS|PDVD|TSRip|HDTSRip|TELECINE|HD-TC|HDTC)\\b"
      + "|\\bTS[-_. ]",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern CAM_REGEX = Pattern.compile(
      "\\b(CAMRIP|NEWCAM|HQCAM)\\b"
      + "|\\bHD-?CAM(?:Rip)?\\b"
      + "|(?<![A-Z])CAM(?![A-Z])",
      Pattern.CASE_INSENSITIVE);

  public int quality() {
    return switch (this) {
      case CAM -> 0;
      case TELESYNC -> 1;
      case DVD -> 2;
      case HDTV -> 3;
      case WEB -> 4;
      case BR -> 5;
    };
  }

  public boolean isLowQuality() {
    return this == CAM || this == TELESYNC;
  }

  /** Returns null if the title doesn't match any known rip type — candidate should be skipped. */
  public static RipType fromTitle(String title) {
    if (title == null) return null;
    if (BR_REGEX.matcher(title).find())       return BR;
    if (WEBDL_REGEX.matcher(title).find())    return WEB;
    if (WEBRIP_REGEX.matcher(title).find())   return WEB;
    if (HDTV_REGEX.matcher(title).find())     return HDTV;
    if (DVD_REGEX.matcher(title).find())      return DVD;
    if (TELESYNC_REGEX.matcher(title).find()) return TELESYNC;
    if (CAM_REGEX.matcher(title).find())      return CAM;
    return null;
  }
}
