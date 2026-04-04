package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.Util;
import com.rsargsyan.probarr.main_ctx.core.app.dto.EpisodeCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.EpisodeDTO;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Episode;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.TVShow;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.BlacklistReason;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.EpisodeRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.TVShowRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EpisodeService {

  private final EpisodeRepository episodeRepository;
  private final TVShowRepository tvShowRepository;
  private final TVShowScanTransactionService tvShowScanTransactionService;

  @Autowired
  public EpisodeService(EpisodeRepository episodeRepository,
                        TVShowRepository tvShowRepository,
                        TVShowScanTransactionService tvShowScanTransactionService) {
    this.episodeRepository = episodeRepository;
    this.tvShowRepository = tvShowRepository;
    this.tvShowScanTransactionService = tvShowScanTransactionService;
  }

  public List<EpisodeDTO> listEpisodes(String tvShowIdStr, Integer seasonNumber) {
    Long tvShowId = Util.validateTSID(tvShowIdStr);
    return episodeRepository.findByTvShowIdAndSeasonNumber(tvShowId, seasonNumber).stream()
        .map(EpisodeDTO::from)
        .toList();
  }

  public EpisodeDTO getEpisode(String tvShowIdStr, String episodeIdStr) {
    Long episodeId = Util.validateTSID(episodeIdStr);
    Episode episode = episodeRepository.findById(episodeId)
        .orElseThrow(ResourceNotFoundException::new);
    return EpisodeDTO.from(episode);
  }

  @Transactional
  public EpisodeDTO createEpisode(String tvShowIdStr, EpisodeCreationDTO dto) {
    Long tvShowId = Util.validateTSID(tvShowIdStr);
    TVShow tvShow = tvShowRepository.findById(tvShowId)
        .orElseThrow(ResourceNotFoundException::new);
    Episode episode = new Episode(tvShow, dto.seasonNumber(), dto.episodeNumber(),
        dto.absoluteNumber(), dto.airDate(), dto.runtimeSeconds());
    episodeRepository.save(episode);
    return EpisodeDTO.from(episode);
  }

  @Transactional
  public EpisodeDTO updateEpisode(String tvShowIdStr, String episodeIdStr, EpisodeCreationDTO dto) {
    Long episodeId = Util.validateTSID(episodeIdStr);
    Episode episode = episodeRepository.findById(episodeId)
        .orElseThrow(ResourceNotFoundException::new);
    episode.update(dto.seasonNumber(), dto.episodeNumber(), dto.absoluteNumber(),
        dto.airDate(), dto.runtimeSeconds());
    episodeRepository.save(episode);
    return EpisodeDTO.from(episode);
  }

  public EpisodeDTO triggerScan(String tvShowIdStr, String episodeIdStr) {
    Long episodeId = Util.validateTSID(episodeIdStr);
    Episode episode = episodeRepository.findById(episodeId)
        .orElseThrow(ResourceNotFoundException::new);
    if (!episode.getReleaseCandidates().isEmpty()) {
      log.info("triggerScan: skipping episode [{}] — {} candidate(s) already pending",
          episodeId, episode.getReleaseCandidates().size());
      return EpisodeDTO.from(episode);
    }
    tvShowScanTransactionService.markEpisodeScanning(episodeId);
    Thread.ofVirtual().start(() -> {
      try {
        tvShowScanTransactionService.scanEpisode(episodeId);
      } catch (Exception e) {
        log.error("Async scan failed for episode {}: {}", episodeId, e.getMessage());
        tvShowScanTransactionService.markEpisodeScanDone(episodeId);
      }
    });
    return EpisodeDTO.from(episodeRepository.findById(episodeId).orElseThrow(ResourceNotFoundException::new));
  }

  @Transactional
  public EpisodeDTO addToBlackList(String tvShowIdStr, String episodeIdStr, String infoHash) {
    Long episodeId = Util.validateTSID(episodeIdStr);
    Episode episode = episodeRepository.findById(episodeId).orElseThrow(ResourceNotFoundException::new);
    episode.addToBlackList(infoHash, BlacklistReason.MANUAL);
    episodeRepository.save(episode);
    return EpisodeDTO.from(episode);
  }

  @Transactional
  public EpisodeDTO removeFromBlackList(String tvShowIdStr, String episodeIdStr, String infoHash) {
    Long episodeId = Util.validateTSID(episodeIdStr);
    Episode episode = episodeRepository.findById(episodeId).orElseThrow(ResourceNotFoundException::new);
    episode.removeFromBlackList(infoHash);
    episodeRepository.save(episode);
    return EpisodeDTO.from(episode);
  }

  @Transactional
  public EpisodeDTO addToWhiteList(String tvShowIdStr, String episodeIdStr, String infoHash) {
    Long episodeId = Util.validateTSID(episodeIdStr);
    Episode episode = episodeRepository.findById(episodeId).orElseThrow(ResourceNotFoundException::new);
    episode.addToWhiteList(infoHash);
    episodeRepository.save(episode);
    return EpisodeDTO.from(episode);
  }

  @Transactional
  public EpisodeDTO removeFromWhiteList(String tvShowIdStr, String episodeIdStr, String infoHash) {
    Long episodeId = Util.validateTSID(episodeIdStr);
    Episode episode = episodeRepository.findById(episodeId).orElseThrow(ResourceNotFoundException::new);
    episode.removeFromWhiteList(infoHash);
    episodeRepository.save(episode);
    return EpisodeDTO.from(episode);
  }

  @Transactional
  public EpisodeDTO addToCoolDown(String tvShowIdStr, String episodeIdStr, String infoHash) {
    Long episodeId = Util.validateTSID(episodeIdStr);
    Episode episode = episodeRepository.findById(episodeId).orElseThrow(ResourceNotFoundException::new);
    episode.addToCoolDown(infoHash);
    episodeRepository.save(episode);
    return EpisodeDTO.from(episode);
  }

  @Transactional
  public EpisodeDTO removeFromCoolDown(String tvShowIdStr, String episodeIdStr, String infoHash) {
    Long episodeId = Util.validateTSID(episodeIdStr);
    Episode episode = episodeRepository.findById(episodeId).orElseThrow(ResourceNotFoundException::new);
    episode.removeFromCoolDown(infoHash);
    episodeRepository.save(episode);
    return EpisodeDTO.from(episode);
  }
}
