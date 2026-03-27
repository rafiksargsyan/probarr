package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

import java.util.Optional;

public enum TorrentTracker {
  RUTRACKER,
  RUTOR,
  X1337,
  THE_PIRATE_BAY,
  OXTORRENT,
  LIMETORRENT,
  DONTORRENT,
  CORSARO_NERO,
  CINECALIDAD,
  RARBG,
  UNKNOWN;

  public static Optional<TorrentTracker> fromJackettName(String name) {
    if (name == null) return Optional.of(UNKNOWN);
    String n = name.toLowerCase();
    if (n.contains("1337x")) return Optional.of(X1337);
    if (n.contains("piratebay") || n.contains("pirate bay") || n.contains("thepiratebay")) return Optional.of(THE_PIRATE_BAY);
    if (n.contains("rarbg")) return Optional.of(RARBG);
    if (n.contains("rutracker")) return Optional.of(RUTRACKER);
    if (n.contains("rutor")) return Optional.of(RUTOR);
    if (n.contains("oxtorrent")) return Optional.of(OXTORRENT);
    if (n.contains("limetorrent")) return Optional.of(LIMETORRENT);
    if (n.contains("dontorrent")) return Optional.of(DONTORRENT);
    if (n.contains("corsaro")) return Optional.of(CORSARO_NERO);
    if (n.contains("cinecalidad")) return Optional.of(CINECALIDAD);
    return Optional.of(UNKNOWN);
  }
}
