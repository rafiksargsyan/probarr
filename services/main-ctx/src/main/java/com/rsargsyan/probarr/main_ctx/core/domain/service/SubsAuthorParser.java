package com.rsargsyan.probarr.main_ctx.core.domain.service;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.SubsAuthor;

import java.util.regex.Pattern;

public class SubsAuthorParser {

  private static final Pattern HDREZKA_18PLUS = Pattern.compile("hdrezka.*18", Pattern.CASE_INSENSITIVE);
  private static final Pattern COOL_STORY_BLOG_18PLUS = Pattern.compile("cool.*story.*blog.*18", Pattern.CASE_INSENSITIVE);
  private static final Pattern COOL_STORY_BLOG = Pattern.compile("cool.*story.*blog", Pattern.CASE_INSENSITIVE);

  public static SubsAuthor parse(String streamTitle) {
    if (streamTitle == null) return null;
    String t = streamTitle.toLowerCase();
    if (HDREZKA_18PLUS.matcher(t).find())       return SubsAuthor.HDREZKA_18PLUS;
    if (COOL_STORY_BLOG_18PLUS.matcher(t).find()) return SubsAuthor.COOL_STORY_BLOG_18PLUS;
    if (t.contains("hdrezka"))                  return SubsAuthor.HDREZKA;
    if (t.contains("tvshows"))                  return SubsAuthor.TVSHOWS;
    if (t.contains("киномания"))                return SubsAuthor.KINOMANIA;
    if (COOL_STORY_BLOG.matcher(t).find())      return SubsAuthor.COOL_STORY_BLOG;
    return null;
  }
}
