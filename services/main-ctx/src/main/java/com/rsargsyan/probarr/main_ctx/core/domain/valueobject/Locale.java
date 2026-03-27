package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

public enum Locale {
  EN_US("en-US"),
  EN_GB("en-GB"),
  EN_AU("en-AU"),
  HY_AM("hy-AM"),
  RU_RU("ru-RU"),
  FR_FR("fr-FR"),
  DE_DE("de-DE"),
  ES_ES("es-ES"),
  ES_MX("es-MX"),
  IT_IT("it-IT"),
  PT_BR("pt-BR"),
  ZH_HANS_CN("zh-Hans-CN"),
  ZH_HANT_TW("zh-Hant-TW"),
  ZH_HANT_HK("zh-Hant-HK");

  private final String tag;

  Locale(String tag) {
    this.tag = tag;
  }

  public String getTag() {
    return tag;
  }

  /** Finds a Locale by its exact BCP 47 tag (e.g. "en-US"). */
  public static java.util.Optional<Locale> fromTag(String tag) {
    for (Locale l : values()) {
      if (l.tag.equalsIgnoreCase(tag)) return java.util.Optional.of(l);
    }
    return java.util.Optional.empty();
  }

  /**
   * Maps an ISO 639-1 language code (e.g. "en") to a default Locale.
   */
  public static java.util.Optional<Locale> fromLanguageCode(String iso639) {
    if (iso639 == null) return java.util.Optional.empty();
    return switch (iso639.toLowerCase()) {
      case "en" -> java.util.Optional.of(EN_US);
      case "hy" -> java.util.Optional.of(HY_AM);
      case "ru" -> java.util.Optional.of(RU_RU);
      case "fr" -> java.util.Optional.of(FR_FR);
      case "de" -> java.util.Optional.of(DE_DE);
      case "es" -> java.util.Optional.of(ES_ES);
      case "it" -> java.util.Optional.of(IT_IT);
      case "pt" -> java.util.Optional.of(PT_BR);
      default -> java.util.Optional.empty();
    };
  }
}
