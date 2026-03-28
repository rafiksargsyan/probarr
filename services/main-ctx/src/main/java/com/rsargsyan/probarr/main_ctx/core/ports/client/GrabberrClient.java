package com.rsargsyan.probarr.main_ctx.core.ports.client;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GrabberrClient {

  TorrentDownloadDTO submitTorrent(String downloadUrl);

  TorrentDownloadDTO submitTorrentFile(byte[] torrentBytes);

  Optional<TorrentDownloadDTO> findByInfoHash(String infoHash);

  FileDownloadDTO claimFile(String torrentDownloadId, int fileIndex);

  FileDownloadDTO getFileStatus(String torrentDownloadId, int fileIndex);

  void unclaimFile(String torrentDownloadId, int fileIndex);

  void deleteTorrentDownload(String torrentDownloadId);

  record TorrentDownloadDTO(String id, String infoHash, TorrentStatus status, List<TorrentFile> files, Instant createdAt) {}

  record FileDownloadDTO(String id, Integer fileIndex, FileDownloadStatus status,
                         Float progress, String signedUrl, Long fileSizeBytes,
                         Instant createdAt, Instant downloadingAt) {}

  record TorrentFile(int index, String name, long sizeBytes) {}

  enum TorrentStatus { QUEUED, FETCHING_METADATA, READY, FAILED }

  enum FileDownloadStatus { SUBMITTED, DOWNLOADING, TRANSFERRING, DONE, FAILED }
}
