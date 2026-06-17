package com.rsargsyan.probarr.main_ctx.core.domain.service;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioAuthor;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioVoiceType;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Locale;

import java.util.regex.Pattern;

public class AudioVoiceTypeParser {

  private static final Pattern DUB_REGEX = Pattern.compile(
      "(^dub$)|(^dub[^a-zA-Z0-9]+)|([^a-zA-Z0-9]+dub$)|([^a-zA-Z0-9]dub[^a-zA-Z0-9])",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern MVO_REGEX = Pattern.compile(
      "(^mvo$)|(^mvo[^a-zA-Z0-9]+)|([^a-zA-Z0-9]+mvo$)|([^a-zA-Z0-9]mvo[^a-zA-Z0-9])",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern DVO_REGEX = Pattern.compile(
      "(^dvo$)|(^dvo[^a-zA-Z0-9]+)|([^a-zA-Z0-9]+dvo$)|([^a-zA-Z0-9]dvo[^a-zA-Z0-9])",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern AVO_REGEX = Pattern.compile(
      "(^avo$)|(^avo[^a-zA-Z0-9]+)|([^a-zA-Z0-9]+avo$)|([^a-zA-Z0-9]avo[^a-zA-Z0-9])",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern VO_REGEX = Pattern.compile(
      "(^vo$)|(^vo[^a-zA-Z0-9]+)|([^a-zA-Z0-9]+vo$)|([^a-zA-Z0-9]vo[^a-zA-Z0-9])",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern SO_REGEX = Pattern.compile(
      "(^so$)|(^so[^a-zA-Z0-9]+)|([^a-zA-Z0-9]+so$)|([^a-zA-Z0-9]so[^a-zA-Z0-9])",
      Pattern.CASE_INSENSITIVE);

  private static final java.util.Set<AudioAuthor> DUB_AUTHORS = java.util.Set.of(
      AudioAuthor.BRAVO_RECORDS_GEORGIA,
      AudioAuthor.READ_HEAD_SOUND,
      AudioAuthor.MOVIE_DUBBING,
      AudioAuthor.MOVIE_DALEN,
      AudioAuthor.POSTMODERN,
      AudioAuthor.PIFAGOR
  );

  /**
   * Resolves voice type from stream title, author, and locale context.
   * If the detected audio language shares a base language with the movie's original locale,
   * it is classified as ORIGINAL without requiring a title hint.
   * Returns null if type cannot be determined.
   */
  public static AudioVoiceType parse(String streamTitle, AudioAuthor author, Locale language, Locale originalLocale) {
    if (streamTitle == null) {
      if (language != null && originalLocale != null) {
        String detectedBase = language.getTag().split("-")[0];
        String originalBase = originalLocale.getTag().split("-")[0];
        if (detectedBase.equals(originalBase)) return AudioVoiceType.ORIGINAL;
      }
      if (author != null && DUB_AUTHORS.contains(author)) return AudioVoiceType.DUB;
      return null;
    }
    String t = streamTitle.toLowerCase();
    if (t.contains("commentary") || t.contains("комментари")) return AudioVoiceType.COMMENTARY;
    if (language != null && originalLocale != null) {
      String detectedBase = language.getTag().split("-")[0];
      String originalBase = originalLocale.getTag().split("-")[0];
      if (detectedBase.equals(originalBase)) return AudioVoiceType.ORIGINAL;
    }
    if (t.contains("дубляж") || t.contains("дублированный") || DUB_REGEX.matcher(t).find()) return AudioVoiceType.DUB;
    if (MVO_REGEX.matcher(t).find() || t.contains("многоголосый")) return AudioVoiceType.MVO;
    if (DVO_REGEX.matcher(t).find() || t.contains("двухголосый")) return AudioVoiceType.DVO;
    if (AVO_REGEX.matcher(t).find() || VO_REGEX.matcher(t).find() || SO_REGEX.matcher(t).find() || t.contains("одноголосый")) return AudioVoiceType.SO;
    if (t.contains("original") || t.contains("оригинал")) return AudioVoiceType.ORIGINAL;
    if (author != null && DUB_AUTHORS.contains(author)) return AudioVoiceType.DUB;
    return null;
  }
}
