package com.rsargsyan.probarr.main_ctx.core.ports.client;

import java.time.Instant;
import java.util.List;

public interface IndexerClient {

  List<IndexerRelease> searchMovies(String title);

  List<IndexerRelease> searchTvShowSeason(String title, int seasonNumber);

  record IndexerRelease(
      String title,
      String tracker,
      String infoUrl,
      String downloadUrl,
      String magnetUri,
      String infoHash,
      Long sizeInBytes,
      Integer seeders,
      Instant publishDate
  ) {}
}
