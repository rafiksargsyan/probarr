package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

import com.rsargsyan.probarr.main_ctx.core.domain.localentity.AudioTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.SubtitleTrack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Release(
    String infoHash,
    String filePath,
    Long fileSizeBytes,
    String videoCodec,
    Resolution resolution,
    RipType ripType,
    Edition edition,
    Integer runtimeSeconds,
    List<AudioTrack> audioTracks,
    List<SubtitleTrack> subtitleTracks,
    Instant addedAt
) {

  // Priority lists per BCP-47 language tag (lower index = higher priority)
  private static final Map<String, List<AudioAuthor>> AUDIO_AUTHOR_PRIORITY = Map.of(
      "ru", List.of(
          AudioAuthor.HDREZKA,
          AudioAuthor.HDREZKA_18PLUS,
          AudioAuthor.LOSTFILM,
          AudioAuthor.TVSHOWS,
          AudioAuthor.VIRUSEPROJECT,
          AudioAuthor.VIRUSEPROJECT_18PLUS,
          AudioAuthor.MOVIE_DALEN,
          AudioAuthor.POSTMODERN,
          AudioAuthor.IVI,
          AudioAuthor.KINOMANIA,
          AudioAuthor.ONE_PLUS_ONE,
          AudioAuthor.KINAKONG,
          AudioAuthor.MOVIE_DUBBING,
          AudioAuthor.PIFAGOR,
          AudioAuthor.NOVAMEDIA,
          AudioAuthor.RENTV,
          AudioAuthor.KRAVEC_RECORDS,
          AudioAuthor.KIRILLICA,
          AudioAuthor.JASKIER,
          AudioAuthor.JASKIER_18PLUS
      )
  );

  /**
   * Compares two releases to determine which is superior.
   * Returns null if they are incomparable (should coexist), positive if r1 is better,
   * negative if r2 is better, 0 if tied (use compare2 as tiebreaker).
   *
   * Releases with different editions are always incomparable.
   */
  public static Integer compare(Release r1, Release r2) {
    if (!Objects.equals(r1.edition(), r2.edition())) return null;

    List<AudioTrack> r1Unmatched = new ArrayList<>(r1.audioTracks());
    List<AudioTrack> r2Unmatched = new ArrayList<>(r2.audioTracks());

    for (AudioTrack a1 : r1.audioTracks()) {
      for (AudioTrack a2 : r2.audioTracks()) {
        Integer cmp = compareAudio(a1, a2);
        if (cmp == null) continue;
        if (cmp <= 0) r1Unmatched.remove(a1);
        if (cmp >= 0) r2Unmatched.remove(a2);
      }
    }

    // Both sides have unique audio not found in the other — incomparable, coexist
    if (!r1Unmatched.isEmpty() && !r2Unmatched.isEmpty()) return null;

    if (r1Unmatched.isEmpty() && r2Unmatched.isEmpty()) {
      // All audios matched — compare by rip quality and resolution
      if (r1.ripType().isLowQuality() || r2.ripType().isLowQuality()) {
        int ripCmp = Integer.compare(r1.ripType().quality(), r2.ripType().quality());
        if (ripCmp != 0) return ripCmp;
      }
      int resCmp = r1.resolution().compareTo(r2.resolution());
      if (resCmp != 0) return resCmp;
    }

    return r1Unmatched.size() - r2Unmatched.size();
  }

  /**
   * Tiebreaker for releases with compare == 0.
   * Positive means r1 is better, negative means r2 is better.
   */
  public static int compare2(Release r1, Release r2) {
    long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
    if (Math.abs(r1.addedAt().toEpochMilli() - r2.addedAt().toEpochMilli()) < sevenDaysMs) {
      int ripCmp = Integer.compare(r1.ripType().quality(), r2.ripType().quality());
      if (ripCmp != 0) return ripCmp;
      int subsCmp = Integer.compare(r1.subtitleTracks().size(), r2.subtitleTracks().size());
      if (subsCmp != 0) return subsCmp;
      return Long.compare(r2.fileSizeBytes(), r1.fileSizeBytes()); // smaller is better
    }
    return r1.addedAt().compareTo(r2.addedAt()); // newer is better
  }

  /**
   * Compares two audio tracks. Returns null if they are not comparable (different
   * languages, or both are commentary tracks). Positive means a1 is better.
   */
  private static Integer compareAudio(AudioTrack a1, AudioTrack a2) {
    if (a1.voiceType() == AudioVoiceType.COMMENTARY || a2.voiceType() == AudioVoiceType.COMMENTARY) return null;
    if (!Objects.equals(a1.language(), a2.language())) return null;

    int voiceCmp = Integer.compare(voiceTypePriority(a1.voiceType()), voiceTypePriority(a2.voiceType()));
    if (voiceCmp != 0) return voiceCmp;

    // Same voice type — compare authors
    if (a1.author() == null && a2.author() == null) return 0;
    if (a1.author() == null) return -1;
    if (a2.author() == null) return 1;
    if (a1.author() == a2.author()) return 0;

    List<AudioAuthor> priorityList = AUDIO_AUTHOR_PRIORITY.getOrDefault(a1.language(), List.of());
    int i1 = priorityList.indexOf(a1.author());
    int i2 = priorityList.indexOf(a2.author());
    if (i1 == -1 && i2 == -1) return null; // both unknown priority, incomparable
    if (i1 == -1) return -1; // a2 in list, a1 not → a2 wins
    if (i2 == -1) return 1;  // a1 in list, a2 not → a1 wins
    return Integer.compare(i2, i1); // lower index = higher priority
  }

  private static int voiceTypePriority(AudioVoiceType v) {
    if (v == null) return -1;
    return switch (v) {
      case ORIGINAL -> 4;
      case DUB -> 3;
      case MVO -> 2;
      case DVO -> 1;
      case SO -> 0;
      case COMMENTARY -> -1;
    };
  }
}
