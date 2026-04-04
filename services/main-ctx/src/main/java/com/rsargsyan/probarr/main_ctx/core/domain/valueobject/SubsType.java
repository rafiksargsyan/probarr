package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

public enum SubsType {
  FORCED,
  FULL,
  SDH;

  public static SubsType fromTitle(String title, boolean isForced) {
    if (isForced) return FORCED;
    if (title == null) return null;
    String t = title.toLowerCase();
    if (t.contains("forced") || t.contains("форсирован") || t.contains("forzados")) return FORCED;
    if (t.contains("sdh") || t.contains("non udenti")) return SDH;
    if (t.contains("full") || t.contains("полные") || t.contains("complets")
        || t.contains("regular") || t.contains("completi") || t.contains("completos")) return FULL;
    return null;
  }
}
