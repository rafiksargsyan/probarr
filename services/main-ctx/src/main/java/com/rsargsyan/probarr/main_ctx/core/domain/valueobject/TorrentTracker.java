package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

import java.util.Optional;

public enum TorrentTracker {
  RUTRACKER_ORG,
  RUTRACKER_RU,
  RUTOR,
  X1337,
  THE_PIRATE_BAY,
  OXTORRENT,
  LIMETORRENT,
  DONTORRENT,
  CORSARO_NERO,
  CINECALIDAD,
  RARBG,
  KINOZAL,
  TORRENT_GALAXY;

  public static Optional<TorrentTracker> fromJackettName(String name) {
    if (name == null) return Optional.empty();
    String n = name.toLowerCase();
    if (n.contains("1337x")) return Optional.of(X1337);
    if (n.contains("piratebay") || n.contains("pirate bay") || n.contains("thepiratebay")) return Optional.of(THE_PIRATE_BAY);
    if (n.contains("rarbg")) return Optional.of(RARBG);
    if (n.contains("rutracker.org")) return Optional.of(RUTRACKER_ORG);
    if (n.contains("rutracker.ru")) return Optional.of(RUTRACKER_RU);
    if (n.contains("rutor")) return Optional.of(RUTOR);
    if (n.contains("oxtorrent")) return Optional.of(OXTORRENT);
    if (n.contains("limetorrent")) return Optional.of(LIMETORRENT);
    if (n.contains("dontorrent")) return Optional.of(DONTORRENT);
    if (n.contains("corsaro")) return Optional.of(CORSARO_NERO);
    if (n.contains("cinecalidad")) return Optional.of(CINECALIDAD);
    if (n.contains("kinozal")) return Optional.of(KINOZAL);
    if (n.contains("torrentgalaxy") || n.contains("torrent galaxy") || n.contains("tgx")) return Optional.of(TORRENT_GALAXY);
    return Optional.empty();
  }
}
