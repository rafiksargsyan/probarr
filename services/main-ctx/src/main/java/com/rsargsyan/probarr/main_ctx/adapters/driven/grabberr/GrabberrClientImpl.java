package com.rsargsyan.probarr.main_ctx.adapters.driven.grabberr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.core.ports.client.GrabberrClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class GrabberrClientImpl implements GrabberrClient {

  private final RestClient restClient;

  @Autowired
  public GrabberrClientImpl(Config config) {
    this.restClient = RestClient.builder()
        .baseUrl(config.grabberrBaseUrl)
        .defaultHeader("X-API-KEY-ID", config.grabberrApiKeyId)
        .defaultHeader("X-API-KEY", config.grabberrApiKey)
        .build();
  }

  @Override
  public TorrentDownloadDTO submitTorrent(String downloadUrl) {
    GrabberrTorrentDownloadDTO dto = restClient.post()
        .uri(b -> b.path("/torrent-download").queryParam("downloadUrl", downloadUrl).build())
        .retrieve()
        .body(GrabberrTorrentDownloadDTO.class);
    return toPort(dto);
  }

  @Override
  public TorrentDownloadDTO submitTorrentFile(byte[] torrentBytes) {
    org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
    body.add("file", new org.springframework.core.io.ByteArrayResource(torrentBytes) {
      @Override public String getFilename() { return "torrent.torrent"; }
    });
    GrabberrTorrentDownloadDTO dto = restClient.post()
        .uri("/torrent-download/upload")
        .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
        .body(body)
        .retrieve()
        .body(GrabberrTorrentDownloadDTO.class);
    return toPort(dto);
  }

  @Override
  public Optional<TorrentDownloadDTO> findByInfoHash(String infoHash) {
    try {
      GrabberrTorrentDownloadDTO dto = restClient.get()
          .uri("/torrent-download/by-hash/{hash}", infoHash)
          .retrieve()
          .body(GrabberrTorrentDownloadDTO.class);
      return Optional.ofNullable(dto).map(this::toPort);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) return Optional.empty();
      throw e;
    }
  }

  @Override
  public FileDownloadDTO claimFile(String torrentDownloadId, int fileIndex) {
    GrabberrFileDownloadDTO dto = restClient.put()
        .uri("/torrent-download/{id}/file/{fileIndex}", torrentDownloadId, fileIndex)
        .retrieve()
        .body(GrabberrFileDownloadDTO.class);
    return toPort(dto);
  }

  @Override
  public FileDownloadDTO getFileStatus(String torrentDownloadId, int fileIndex) {
    try {
      GrabberrFileDownloadDTO dto = restClient.get()
          .uri("/torrent-download/{id}/file/{fileIndex}", torrentDownloadId, fileIndex)
          .retrieve()
          .body(GrabberrFileDownloadDTO.class);
      return toPort(dto);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
      throw e;
    }
  }

  @Override
  public void unclaimFile(String torrentDownloadId, int fileIndex) {
    restClient.delete()
        .uri("/torrent-download/{id}/file/{fileIndex}", torrentDownloadId, fileIndex)
        .retrieve()
        .toBodilessEntity();
  }

  @Override
  public void deleteTorrentDownload(String torrentDownloadId) {
    restClient.delete()
        .uri("/torrent-download/{id}", torrentDownloadId)
        .retrieve()
        .toBodilessEntity();
  }

  private TorrentDownloadDTO toPort(GrabberrTorrentDownloadDTO dto) {
    if (dto == null) return null;
    List<TorrentFile> files = dto.files() == null ? List.of() :
        dto.files().stream().map(f -> new TorrentFile(f.index(), f.name(), f.sizeBytes())).toList();
    return new TorrentDownloadDTO(dto.id(), dto.infoHash(), TorrentStatus.valueOf(dto.status()), files, dto.createdAt());
  }

  private FileDownloadDTO toPort(GrabberrFileDownloadDTO dto) {
    if (dto == null) return null;
    return new FileDownloadDTO(dto.id(), dto.fileIndex(), FileDownloadStatus.valueOf(dto.status()),
        dto.progress(), dto.signedUrl(), dto.fileSizeBytes(),
        dto.createdAt(), dto.downloadingAt());
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GrabberrTorrentDownloadDTO(
      @JsonProperty("id") String id,
      @JsonProperty("infoHash") String infoHash,
      @JsonProperty("status") String status,
      @JsonProperty("files") List<GrabberrTorrentFile> files,
      @JsonProperty("createdAt") Instant createdAt
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GrabberrTorrentFile(
      @JsonProperty("index") int index,
      @JsonProperty("name") String name,
      @JsonProperty("sizeBytes") long sizeBytes
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GrabberrFileDownloadDTO(
      @JsonProperty("id") String id,
      @JsonProperty("fileIndex") Integer fileIndex,
      @JsonProperty("status") String status,
      @JsonProperty("progress") Float progress,
      @JsonProperty("signedUrl") String signedUrl,
      @JsonProperty("fileSizeBytes") Long fileSizeBytes,
      @JsonProperty("createdAt") Instant createdAt,
      @JsonProperty("downloadingAt") Instant downloadingAt
  ) {}
}
