package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

public enum Edition {
  THEATRICAL,
  EXTENDED,
  DIRECTORS_CUT,
  UNRATED,
  REMASTERED,
  IMAX,
  SPECIAL_EDITION,
  CRITERION;

  public static Edition fromTitle(String title) {
    if (title == null) return null;
    String t = title.toLowerCase();
    if (t.contains("imax")) return IMAX;
    if (t.contains("director") && (t.contains("cut") || t.contains("edition"))) return DIRECTORS_CUT;
    if (t.contains("extended")) return EXTENDED;
    if (t.contains("unrated")) return UNRATED;
    if (t.contains("criterion")) return CRITERION;
    if (t.contains("remaster")) return REMASTERED;
    if (t.contains("special edition")) return SPECIAL_EDITION;
    if (t.contains("theatrical")) return THEATRICAL;
    return null;
  }
}
