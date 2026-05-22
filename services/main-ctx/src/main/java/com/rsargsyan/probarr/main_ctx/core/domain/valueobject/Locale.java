package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Locale {
  EN("en"),
  EN_US("en-US"),
  EN_GB("en-GB"),
  EN_AU("en-AU"),
  RU("ru"),
  FR("fr"),
  FR_FR("fr-FR"),
  FR_CA("fr-CA"),
  DE("de"),
  ES("es"),
  ES_ES("es-ES"),
  ES_419("es-419"),
  IT("it"),
  PT("pt"),
  PT_BR("pt-BR"),
  PT_PT("pt-PT"),
  JA("ja"),
  KO("ko"),
  ZH("zh"),
  ZH_HANS_CN("zh-Hans-CN"),
  ZH_HANT_TW("zh-Hant-TW"),
  ZH_HANT_HK("zh-Hant-HK"),
  HI("hi"),
  TA("ta"),
  TE("te"),
  ML("ml"),
  UR("ur"),
  BN("bn"),
  TR("tr"),
  EL("el"),
  TH("th"),
  ID("id"),
  MS("ms"),
  VI("vi"),
  TL("tl"),
  SQ("sq"),
  AZ("az"),
  KA("ka"),
  DA("da"),
  SV("sv"),
  NB("nb"),
  NL("nl"),
  NL_NL("nl-NL"),
  NL_BE("nl-BE"),
  PL("pl"),
  UK("uk"),
  BE("be"),
  BG("bg"),
  CS("cs"),
  SK("sk"),
  SL("sl"),
  HR("hr"),
  SR("sr"),
  BS("bs"),
  MK("mk"),
  HU("hu"),
  RO("ro"),
  ET("et"),
  LV("lv"),
  LT("lt"),
  FI("fi"),
  HY("hy"),
  FA("fa"),
  AR("ar"),
  HE("he"),
  MYN("myn");

  private final String tag;

  Locale(String tag) {
    this.tag = tag;
  }

  @JsonValue
  public String getTag() {
    return tag;
  }

  @JsonCreator
  public static Locale fromString(String value) {
    if (value == null) return null;
    for (Locale l : values()) {
      if (l.tag.equalsIgnoreCase(value)) return l;
    }
    return fromISO639_2(value).orElseThrow(() -> new IllegalArgumentException("Unknown locale: " + value));
  }

  public static java.util.Optional<Locale> fromTag(String tag) {
    if (tag == null) return java.util.Optional.empty();
    for (Locale l : values()) {
      if (l.tag.equalsIgnoreCase(tag)) return java.util.Optional.of(l);
    }
    return java.util.Optional.empty();
  }

  public static java.util.Optional<Locale> fromISO639_1(String code) {
    if (code == null) return java.util.Optional.empty();
    return java.util.Optional.ofNullable(switch (code.toLowerCase()) {
      case "en" -> EN;
      case "ru" -> RU;
      case "fr" -> FR;
      case "de" -> DE;
      case "es" -> ES;
      case "it" -> IT;
      case "pt" -> PT;
      case "ja" -> JA;
      case "ko" -> KO;
      case "zh" -> ZH;
      case "hi" -> HI;
      case "ta" -> TA;
      case "da" -> DA;
      case "sv" -> SV;
      case "nb" -> NB;
      case "nl" -> NL;
      case "bs" -> BS;
      case "pl" -> PL;
      case "uk" -> UK;
      case "be" -> BE;
      case "bg" -> BG;
      case "cs" -> CS;
      case "sk" -> SK;
      case "sl" -> SL;
      case "hr" -> HR;
      case "sr" -> SR;
      case "mk" -> MK;
      case "hu" -> HU;
      case "ro" -> RO;
      case "et" -> ET;
      case "lv" -> LV;
      case "lt" -> LT;
      case "fi" -> FI;
      case "hy" -> HY;
      case "fa" -> FA;
      case "ar" -> AR;
      case "he" -> HE;
      case "tr" -> TR;
      case "el" -> EL;
      case "th" -> TH;
      case "id" -> ID;
      case "ms" -> MS;
      case "vi" -> VI;
      case "tl" -> TL;
      case "bn" -> BN;
      case "te" -> TE;
      case "ur" -> UR;
      case "ml" -> ML;
      case "sq" -> SQ;
      case "az" -> AZ;
      case "ka" -> KA;
      default -> null;
    });
  }

  public static java.util.Optional<Locale> fromISO639_2(String code) {
    if (code == null) return java.util.Optional.empty();
    return java.util.Optional.ofNullable(switch (code.toLowerCase()) {
      case "eng" -> EN;
      case "rus" -> RU;
      case "fra", "fre" -> FR;
      case "ger", "deu" -> DE;
      case "spa" -> ES;
      case "ita" -> IT;
      case "por" -> PT;
      case "jpn" -> JA;
      case "kor" -> KO;
      case "chi", "zho" -> ZH;
      case "hin" -> HI;
      case "tam" -> TA;
      case "dan" -> DA;
      case "swe" -> SV;
      case "nob", "nor" -> NB;
      case "nld", "dut" -> NL;
      case "bos" -> BS;
      case "pol" -> PL;
      case "ukr" -> UK;
      case "bel" -> BE;
      case "bul" -> BG;
      case "cze", "ces" -> CS;
      case "slk", "slo" -> SK;
      case "slv" -> SL;
      case "hrv" -> HR;
      case "srp" -> SR;
      case "mac", "mkd" -> MK;
      case "hun" -> HU;
      case "ron", "rum" -> RO;
      case "est" -> ET;
      case "lav" -> LV;
      case "lit" -> LT;
      case "fin" -> FI;
      case "arm", "hye" -> HY;
      case "fas", "per" -> FA;
      case "ara" -> AR;
      case "heb" -> HE;
      case "myn" -> MYN;
      case "tur" -> TR;
      case "ell", "gre" -> EL;
      case "tha" -> TH;
      case "ind" -> ID;
      case "may", "msa" -> MS;
      case "vie" -> VI;
      case "tgl" -> TL;
      case "ben" -> BN;
      case "tel" -> TE;
      case "urd" -> UR;
      case "mal" -> ML;
      case "sqi", "alb" -> SQ;
      case "aze" -> AZ;
      case "kat", "geo" -> KA;
      default -> null;
    });
  }
}
