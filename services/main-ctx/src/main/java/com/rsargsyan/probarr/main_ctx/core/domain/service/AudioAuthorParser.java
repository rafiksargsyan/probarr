package com.rsargsyan.probarr.main_ctx.core.domain.service;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.AudioAuthor;

import java.util.regex.Pattern;

public class AudioAuthorParser {

  private static final Pattern HDREZKA_18PLUS = Pattern.compile("hdrezka.*18", Pattern.CASE_INSENSITIVE);
  private static final Pattern VIRUSEPROJECT_18PLUS = Pattern.compile("viruseproject.*18", Pattern.CASE_INSENSITIVE);
  private static final Pattern JASKIER_18PLUS = Pattern.compile("jaskier.*18", Pattern.CASE_INSENSITIVE);
  private static final Pattern BRAVO_RECORDS_GEORGIA = Pattern.compile("bravo.*records.*georgia", Pattern.CASE_INSENSITIVE);
  private static final Pattern READ_HEAD_SOUND = Pattern.compile("read.*head.*sound", Pattern.CASE_INSENSITIVE);
  private static final Pattern MOVIE_DUBBING = Pattern.compile("movie.*dubbing", Pattern.CASE_INSENSITIVE);

  public static AudioAuthor parse(String streamTitle) {
    if (streamTitle == null) return null;
    String t = streamTitle.toLowerCase();
    if (HDREZKA_18PLUS.matcher(t).find())      return AudioAuthor.HDREZKA_18PLUS;
    if (VIRUSEPROJECT_18PLUS.matcher(t).find()) return AudioAuthor.VIRUSEPROJECT_18PLUS;
    if (JASKIER_18PLUS.matcher(t).find())      return AudioAuthor.JASKIER_18PLUS;
    if (t.contains("hdrezka"))                 return AudioAuthor.HDREZKA;
    if (t.contains("viruseproject"))           return AudioAuthor.VIRUSEPROJECT;
    if (t.contains("moviedalen"))              return AudioAuthor.MOVIE_DALEN;
    if (t.contains("postmodern"))              return AudioAuthor.POSTMODERN;
    if (t.contains("tvshows"))                 return AudioAuthor.TVSHOWS;
    if (t.contains("lostfilm"))                return AudioAuthor.LOSTFILM;
    if (BRAVO_RECORDS_GEORGIA.matcher(t).find()) return AudioAuthor.BRAVO_RECORDS_GEORGIA;
    if (READ_HEAD_SOUND.matcher(t).find())     return AudioAuthor.READ_HEAD_SOUND;
    if (MOVIE_DUBBING.matcher(t).find())       return AudioAuthor.MOVIE_DUBBING;
    if (t.contains("кириллица"))               return AudioAuthor.KIRILLICA;
    if (t.contains("киномания"))               return AudioAuthor.KINOMANIA;
    if (t.contains("1+1"))                     return AudioAuthor.ONE_PLUS_ONE;
    if (t.contains("ivi"))                     return AudioAuthor.IVI;
    if (t.contains("jaskier"))                 return AudioAuthor.JASKIER;
    if (t.contains("кінаконг"))                return AudioAuthor.KINAKONG;
    if (t.contains("пифагор"))                 return AudioAuthor.PIFAGOR;
    if (t.contains("novamedia"))               return AudioAuthor.NOVAMEDIA;
    if (t.contains("ren-tv"))                  return AudioAuthor.RENTV;
    if (t.contains("кравец") || t.contains("kravec")) return AudioAuthor.KRAVEC_RECORDS;
    return null;
  }
}
