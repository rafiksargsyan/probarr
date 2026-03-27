package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

public enum RipType {
  BLURAY,
  WEBDL,
  WEBRIP,
  HDTV,
  DVDRIP,
  CAM,
  UNKNOWN;

  public static RipType fromTitle(String title) {
    if (title == null) return UNKNOWN;
    String t = title.toLowerCase();
    if (t.contains("bluray") || t.contains("blu-ray") || t.contains("bdrip") || t.contains("bdremux")) return BLURAY;
    if (t.contains("web-dl") || t.contains("webdl")) return WEBDL;
    if (t.contains("webrip") || t.contains("web-rip")) return WEBRIP;
    if (t.contains("hdtv")) return HDTV;
    if (t.contains("dvdrip") || t.contains("dvdscr") || t.contains("dvd")) return DVDRIP;
    if (t.contains("camrip") || t.contains("hdcam") || t.contains("hdts")
        || t.contains(".cam.") || t.contains(".ts.") || t.contains("-ts-")) return CAM;
    return UNKNOWN;
  }
}
