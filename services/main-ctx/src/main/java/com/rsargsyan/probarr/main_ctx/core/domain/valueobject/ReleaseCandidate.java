package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

import java.time.Instant;
import java.util.List;

public record ReleaseCandidate(
    String infoHash,
    String downloadUrl,
    String infoUrl,
    TorrentTracker tracker,
    Long sizeInBytes,
    Integer seeders,
    Resolution resolution,
    RipType ripType,
    Edition edition,
    Instant releaseAt,
    List<Language> languages,
    String title,
    // Set once grabberr has assigned an id for this candidate's torrent, so subsequent polling
    // can look it up by id instead of by infoHash - grabberr's own resolved infoHash for a
    // magnet/torrent can differ from what the indexer originally reported at search time, which
    // would otherwise make the candidate unfindable forever after a successful submission.
    String torrentDownloadId
) {
  public ReleaseCandidate(String infoHash, String downloadUrl, String infoUrl, TorrentTracker tracker,
                          Long sizeInBytes, Integer seeders, Resolution resolution, RipType ripType,
                          Edition edition, Instant releaseAt, List<Language> languages, String title) {
    this(infoHash, downloadUrl, infoUrl, tracker, sizeInBytes, seeders, resolution, ripType,
        edition, releaseAt, languages, title, null);
  }

  public ReleaseCandidate withTorrentDownloadId(String torrentDownloadId) {
    return new ReleaseCandidate(infoHash, downloadUrl, infoUrl, tracker, sizeInBytes, seeders,
        resolution, ripType, edition, releaseAt, languages, title, torrentDownloadId);
  }
}
