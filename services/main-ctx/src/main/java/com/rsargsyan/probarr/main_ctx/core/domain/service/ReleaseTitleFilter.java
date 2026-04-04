package com.rsargsyan.probarr.main_ctx.core.domain.service;

import java.util.regex.Pattern;

/**
 * Rejects releases matching Radarr custom formats configured with score -10000
 * (BR-DISK, 3D, Extras, LQ, LQ Release Title, Upscaled, DV, Trailer)
 * plus the Raw-HD quality which was disabled in the quality profile.
 */
public class ReleaseTitleFilter {

  // --- AV1 ---
  private static final Pattern AV1 = Pattern.compile(
      "\\bAV1\\b",
      Pattern.CASE_INSENSITIVE
  );

  // --- BR-DISK ---
  private static final Pattern BR_DISK = Pattern.compile(
      "^(?!.*\\b((?<!HD[._ -]|HD)DVD|BDRip|720p|MKV|XviD|WMV|d3g|(BD)?REMUX" +
      "|^(?=.*1080p)(?=.*HEVC)|[xh][-_. ]?26[45]|German.*[DM]L" +
      "|((?<=\\d{4}).*German.*([DM]L)?)(?=.*\\b(AVC|HEVC|VC[-_. ]?1|MVC|MPEG[-_. ]?2)\\b))\\b)" +
      "(((?=.*\\b(Blu[-_. ]?ray|BD|HD[-_. ]?DVD)\\b)(?=.*\\b(AVC|HEVC|VC[-_. ]?1|MVC|MPEG[-_. ]?2|BDMV|ISO)\\b))" +
      "|^((?=.*\\b(((?=.*\\b((.*_)?COMPLETE.*|Dis[ck])\\b)(?=.*(Blu[-_. ]?ray|HD[-_. ]?DVD)))" +
      "|3D[-_. ]?BD|BR[-_. ]?DISK|Full[-_. ]?Blu[-_. ]?ray|^((?=.*((BD|UHD)[-_. ]?(25|50|66|100|ISO)))))))).*",
      Pattern.CASE_INSENSITIVE
  );

  // --- 3D ---
  private static final Pattern THREE_D = Pattern.compile(
      "(?<=\\b[12]\\d{3}\\b).*\\b(3d|sbs|half[ .-]ou|half[ .-]sbs)\\b" +
      "|\\b(BluRay3D|BD3D)\\b",
      Pattern.CASE_INSENSITIVE
  );

  // --- Extras ---
  private static final Pattern EXTRAS = Pattern.compile(
      "(?<=\\b[12]\\d{3}\\b).*\\b(Extras|Bonus|Extended[ ._-]Clip)\\b" +
      "|(?<=\\bS\\d+\\b).*\\b(Extras|Bonus|Extended[ ._-]Clip)\\b",
      Pattern.CASE_INSENSITIVE
  );

  // --- LQ (Release Title) ---
  private static final Pattern LQ_TITLE = Pattern.compile(
      "\\b(1XBET)\\b" +
      "|\\b(BEN[ ._-]THE[ ._-]MEN)\\b" +
      "|(?=.*?(\\b2160p\\b))(?=.*?(\\bBiTOR\\b))" +
      "|(?<!-)\\b(jennaortega(UHD)?)\\b" +
      "|\\b(TeeWee)\\b" +
      "|\\b(Will1869)\\b",
      Pattern.CASE_INSENSITIVE
  );

  // --- LQ (Release Group) ---
  private static final Pattern LQ_GROUP = Pattern.compile(
      "(?i)^(24xHD|41RGB|4K4U|AROMA|aXXo|AZAZE|BARC0DE|BdC|beAst|BRiNK|C4K|CDDHD|CHAOS|CHD|CHX|CiNE" +
      "|CREATiVE24|CrEwSaDe|CTFOH|d3g|DDR|DepraveD|DNL|EPiC|EuReKA|EVO|FaNGDiNG0|FGT|FRDS|FZHD" +
      "|GalaxyRG|GHD|GHOSTS|GPTHD|HDS|HDT|HDTime|HDWinG|HiQVE|iNTENSO|iPlanet|iVy|jennaortega(UHD)?" +
      "|JFF|KiNGDOM|KIRA|L0SERNIGHT|LAMA|Leffe|Liber8|LiGaS|MarkII|MeGusta|mHD|mSD|MTeam|MT" +
      "|MySiLU|NhaNc3|nHD|nikt0|nSD|OFT|PATOMiEL|PiRaTeS|PRODJi|PSA|PTNK|RARBG|RDN|Rifftrax" +
      "|RU4HD|SANTi|SasukeducK|Scene|ShieldBearer|STUTTERSHIT|SWTYBLZ|tarunk9c|TBS|TG|TEKNO3D" +
      "|Tigole|TIKO|VIDEOHOLE|VISIONPLUSHDR(-X|1000)?|WAF|WiKi|worldmkv|x0r|XLF|YIFY|YTS(\\.(MX|LT|AG))?|Zero00|Zeus)$"
  );

  // Pahe and NoGroup have partial match patterns — keep them separate
  private static final Pattern LQ_GROUP_PARTIAL = Pattern.compile(
      "(?i)(Pahe(\\.(ph|in))?\\b|NoGr(ou)?p|\\b24xHD\\b)"
  );

  // --- Upscaled ---
  private static final Pattern UPSCALED = Pattern.compile(
      "(?=.*\\b(HEVC)\\b)(?=.*\\b(AI)\\b)" +
      "|\\b(Re-?grade)\\b" +
      "|\\b(The[ ._-]?Upscaler)\\b" +
      "|(?<=\\b[12]\\d{3}\\b).*\\b(UPS|Up(s(caled?|UHD)|(Rez)))\\b",
      Pattern.CASE_INSENSITIVE
  );

  // --- DV (Dolby Vision) ---
  private static final Pattern DV = Pattern.compile(
      "\\b(dv|dovi|dolby[ .]?v(ision)?)\\b",
      Pattern.CASE_INSENSITIVE
  );

  // --- Trailer (title part; size check done separately) ---
  private static final Pattern TRAILER_TITLE = Pattern.compile(
      "trailer|\u0442\u0440\u0435\u0439\u043b\u0435\u0440|\u0442\u0438\u0437\u0435\u0440|tizer|soundtrack",
      Pattern.CASE_INSENSITIVE
  );

  private static final long TWO_GB = 2L * 1024 * 1024 * 1024;

  /**
   * Returns the name of the matching format/quality if the release should be rejected, or null if it passes.
   */
  public static String reject(String title, Long sizeBytes) {
    if (title == null) return null;

    if (AV1.matcher(title).find())        return "AV1";
    if (BR_DISK.matcher(title).find())   return "BR-DISK";
    if (THREE_D.matcher(title).find())   return "3D";
    if (EXTRAS.matcher(title).find())    return "Extras";
    if (LQ_TITLE.matcher(title).find())  return "LQ (Release Title)";
    if (UPSCALED.matcher(title).find())  return "Upscaled";
    if (DV.matcher(title).find())        return "DV";

    if (TRAILER_TITLE.matcher(title).find() && sizeBytes != null && sizeBytes < TWO_GB)
      return "Trailer";

    String group = extractGroup(title);
    if (group != null) {
      if (LQ_GROUP.matcher(group).matches())         return "LQ";
      if (LQ_GROUP_PARTIAL.matcher(group).find())    return "LQ";
    }

    return null;
  }

  private static String extractGroup(String title) {
    int lastDash = title.lastIndexOf('-');
    if (lastDash < 0 || lastDash == title.length() - 1) return null;
    String group = title.substring(lastDash + 1).trim();
    // Strip trailing junk like [hash], (1080p), etc.
    group = group.replaceAll("[\\[({].*", "").trim();
    return group.isEmpty() ? null : group;
  }
}
