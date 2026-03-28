package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Resolution {
  SD,
  HD_720P,
  FHD_1080P,
  UHD_4K,
  UHD_8K;

  // Mirrors Radarr's ResolutionRegex
  private static final Pattern RESOLUTION_REGEX = Pattern.compile(
      "\\b(?:" +
      "(?<R480p>480p|480i|640x480|848x480)" +
      "|(?<R576p>576p)" +
      "|(?<R720p>720p|1280x720|960p)" +
      "|(?<R1080p>1080p|1920x1080|1440p|FHD|1080i|4kto1080p)" +
      "|(?<R2160p>2160p|3840x2160|4k[-_. ](?:UHD|HEVC|BD|H\\.?265)|(?:UHD|HEVC|BD|H\\.?265)[-_. ]4k)" +
      ")\\b",
      Pattern.CASE_INSENSITIVE);

  // Mirrors Radarr's AlternativeResolutionRegex — fallback when primary finds nothing
  private static final Pattern ALT_RESOLUTION_REGEX = Pattern.compile(
      "\\bUHD\\b|\\[4K\\]",
      Pattern.CASE_INSENSITIVE);

  /**
   * Returns null if resolution cannot be determined from the title.
   * Callers should default to SD for low-quality rip types, or reject for others.
   */
  public static Resolution fromTitle(String title) {
    if (title == null) return null;
    Matcher m = RESOLUTION_REGEX.matcher(title);
    if (m.find()) {
      if (m.group("R2160p") != null) return UHD_4K;
      if (m.group("R1080p") != null) return FHD_1080P;
      if (m.group("R720p")  != null) return HD_720P;
      // R480p, R576p → SD
      return SD;
    }
    if (ALT_RESOLUTION_REGEX.matcher(title).find()) return UHD_4K;
    return null;
  }
}
