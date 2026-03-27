package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

public enum Resolution {
  SD,
  HD_720P,
  FHD_1080P,
  UHD_4K,
  UHD_8K;

  public static Resolution fromTitle(String title) {
    if (title == null) return SD;
    String t = title.toLowerCase();
    if (t.contains("2160p") || t.contains("4k") || t.contains("uhd")) return UHD_4K;
    if (t.contains("1080p") || t.contains("1080i")) return FHD_1080P;
    if (t.contains("720p") || t.contains("720i")) return HD_720P;
    return SD;
  }
}
