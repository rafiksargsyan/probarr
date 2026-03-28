package com.rsargsyan.probarr.main_ctx.core.domain.service;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TitleLanguageParser {

  // Case-insensitive regex for language tokens in titles
  private static final Pattern LANGUAGE_REGEX = Pattern.compile(
      "(?i)\\beng\\b" +
      "|\\b(?:ita|italian)\\b" +
      "|(?:swiss)?german\\b|videomann|ger[. ]dub|\\bger\\b" +
      "|flemish" +
      "|bgaudio" +
      "|rodubbed" +
      "|\\b(?:dublado|pt-BR)\\b" +
      "|greek" +
      "|\\b(?:FR|VO|VF|VFF|VFQ|VFI|VF2|TRUEFRENCH|FRENCH|FRE|FRA)\\b" +
      "|\\b(?:rus|ru)\\b" +
      "|\\b(?:HUNDUB|HUN)\\b" +
      "|\\b(?:HebDub|HebDubbed)\\b" +
      "|\\bPL[\\W]?DUB\\b|\\bDUB[\\W]?PL\\b|\\bLEK[\\W]?PL\\b|\\bPL[\\W]?LEK\\b" +
      "|\\[(?:CH[ST]|BIG5|GB)\\]|[\u7b80\u7e41\u5b57\u5e55]" +
      "|(?:\\dx)?UKR" +
      "|\\b(?:espa\u00f1ol|castellano)\\b" +
      "|\\btel\\b" +
      "|\\bVIE\\b" +
      "|\\bJAP\\b" +
      "|\\bKOR\\b" +
      "|\\burdu\\b" +
      "|\\b(?:orig|original)\\b"
  );

  // Case-sensitive regex for short ISO codes
  private static final Pattern CASE_SENSITIVE_REGEX = Pattern.compile(
      "(?<!SUB[\\W_^])(?:\\bEN\\b|\\bLT\\b|\\bCZ\\b|\\bPL\\b|\\bBG\\b|\\bSK\\b|\\bDE\\b|(?<!DTS[._ -])\\bES\\b)(?![\\W_^]SUB)"
  );

  private static final Pattern GERMAN_DL = Pattern.compile("(?i)\\bDL\\b");
  private static final Pattern GERMAN_ML = Pattern.compile("(?i)\\bML\\b");

  public static List<Language> parse(String title) {
    if (title == null || title.isBlank()) return List.of(Language.UNKNOWN);

    List<Language> languages = new ArrayList<>();
    String lower = title.toLowerCase();

    // Substring checks
    if (lower.contains("english"))         addUnique(languages, Language.ENGLISH);
    if (lower.contains("french"))          addUnique(languages, Language.FRENCH);
    if (lower.contains("truefrench"))      addUnique(languages, Language.FRENCH);
    if (lower.contains("german"))          addUnique(languages, Language.GERMAN);
    if (lower.contains("spanish"))         addUnique(languages, Language.SPANISH);
    if (lower.contains("italian"))         addUnique(languages, Language.ITALIAN);
    if (lower.contains("dutch"))           addUnique(languages, Language.DUTCH);
    if (lower.contains("flemish"))         addUnique(languages, Language.DUTCH);
    if (lower.contains("portuguese"))      addUnique(languages, Language.PORTUGUESE);
    if (lower.contains("brazilian"))       addUnique(languages, Language.PORTUGUESE_BR);
    if (lower.contains("latino"))          addUnique(languages, Language.SPANISH_LATINO);
    if (lower.contains("russian"))         addUnique(languages, Language.RUSSIAN);
    if (lower.contains("hungarian"))       addUnique(languages, Language.HUNGARIAN);
    if (lower.contains("romanian"))        addUnique(languages, Language.ROMANIAN);
    if (lower.contains("czech"))           addUnique(languages, Language.CZECH);
    if (lower.contains("polish"))          addUnique(languages, Language.POLISH);
    if (lower.contains("hebrew"))          addUnique(languages, Language.HEBREW);
    if (lower.contains("greek"))           addUnique(languages, Language.GREEK);
    if (lower.contains("turkish"))         addUnique(languages, Language.TURKISH);
    if (lower.contains("arabic"))          addUnique(languages, Language.ARABIC);
    if (lower.contains("hindi"))           addUnique(languages, Language.HINDI);
    if (lower.contains("chinese"))         addUnique(languages, Language.CHINESE);
    if (lower.contains("japanese"))        addUnique(languages, Language.JAPANESE);
    if (lower.contains("korean"))          addUnique(languages, Language.KOREAN);
    if (lower.contains("vietnamese"))      addUnique(languages, Language.VIETNAMESE);
    if (lower.contains("thai"))            addUnique(languages, Language.THAI);
    if (lower.contains("ukrainian"))       addUnique(languages, Language.UKRAINIAN);
    if (lower.contains("norwegian"))       addUnique(languages, Language.NORWEGIAN);
    if (lower.contains("swedish"))         addUnique(languages, Language.SWEDISH);
    if (lower.contains("danish"))          addUnique(languages, Language.DANISH);
    if (lower.contains("finnish"))         addUnique(languages, Language.FINNISH);

    // Case-insensitive regex
    var matcher = LANGUAGE_REGEX.matcher(title);
    while (matcher.find()) {
      String match = matcher.group().toLowerCase().trim();
      Language lang = matchToLanguage(match);
      if (lang != null) addUnique(languages, lang);
    }

    // Case-sensitive regex
    var csMatcher = CASE_SENSITIVE_REGEX.matcher(title);
    while (csMatcher.find()) {
      Language lang = switch (csMatcher.group()) {
        case "EN" -> Language.ENGLISH;
        case "LT" -> Language.LITHUANIAN;
        case "CZ" -> Language.CZECH;
        case "PL" -> Language.POLISH;
        case "BG" -> Language.BULGARIAN;
        case "SK" -> Language.SLOVAK;
        case "DE" -> Language.GERMAN;
        case "ES" -> Language.SPANISH;
        default -> null;
      };
      if (lang != null) addUnique(languages, lang);
    }

    // German DL/ML special case
    if (languages.size() == 1 && languages.get(0) == Language.GERMAN) {
      if (GERMAN_DL.matcher(title).find()) {
        addUnique(languages, Language.ORIGINAL);
      } else if (GERMAN_ML.matcher(title).find()) {
        addUnique(languages, Language.ORIGINAL);
        addUnique(languages, Language.ENGLISH);
      }
    }

    if (languages.isEmpty()) languages.add(Language.UNKNOWN);
    return List.copyOf(languages);
  }

  private static Language matchToLanguage(String match) {
    if (match.equals("eng")) return Language.ENGLISH;
    if (match.equals("ita") || match.equals("italian")) return Language.ITALIAN;
    if (match.contains("german") || match.equals("videomann") || match.startsWith("ger")) return Language.GERMAN;
    if (match.equals("flemish")) return Language.DUTCH;
    if (match.equals("bgaudio")) return Language.BULGARIAN;
    if (match.equals("rodubbed")) return Language.ROMANIAN;
    if (match.equals("dublado") || match.equals("pt-br")) return Language.PORTUGUESE_BR;
    if (match.equals("greek")) return Language.GREEK;
    if (match.matches("fr|vo|vf|vff|vfq|vfi|vf2|truefrench|french|fre|fra")) return Language.FRENCH;
    if (match.equals("rus") || match.equals("ru")) return Language.RUSSIAN;
    if (match.equals("hundub") || match.equals("hun")) return Language.HUNGARIAN;
    if (match.equals("hebdub") || match.equals("hebdubbed")) return Language.HEBREW;
    if (match.contains("pl") && (match.contains("dub") || match.contains("lek"))) return Language.POLISH;
    if (match.equals("ukr") || match.endsWith("ukr")) return Language.UKRAINIAN;
    if (match.equals("español") || match.equals("castellano")) return Language.SPANISH;
    if (match.equals("tel")) return Language.TELUGU;
    if (match.equals("vie")) return Language.VIETNAMESE;
    if (match.equals("jap")) return Language.JAPANESE;
    if (match.equals("kor")) return Language.KOREAN;
    if (match.equals("urdu")) return Language.URDU;
    if (match.equals("orig") || match.equals("original")) return Language.ORIGINAL;
    // Chinese unicode markers
    if (match.contains("[chs]") || match.contains("[cht]") || match.contains("[big5]") || match.contains("[gb]")
        || match.contains("\u7b80") || match.contains("\u7e41") || match.contains("\u5b57\u5e55")) return Language.CHINESE;
    return null;
  }

  private static void addUnique(List<Language> list, Language lang) {
    if (!list.contains(lang)) list.add(lang);
  }
}
