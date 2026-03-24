package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.Util;
import com.rsargsyan.probarr.main_ctx.core.app.dto.EpisodeCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.EpisodeDTO;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Episode;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.TVShow;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.EpisodeRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.TVShowRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EpisodeService {

  private final EpisodeRepository episodeRepository;
  private final TVShowRepository tvShowRepository;

  @Autowired
  public EpisodeService(EpisodeRepository episodeRepository, TVShowRepository tvShowRepository) {
    this.episodeRepository = episodeRepository;
    this.tvShowRepository = tvShowRepository;
  }

  public List<EpisodeDTO> listEpisodes(String tvShowIdStr, Integer seasonNumber) {
    Long tvShowId = Util.validateTSID(tvShowIdStr);
    return episodeRepository.findByTvShowIdAndSeasonNumber(tvShowId, seasonNumber).stream()
        .map(EpisodeDTO::from)
        .toList();
  }

  @Transactional
  public EpisodeDTO createEpisode(String tvShowIdStr, EpisodeCreationDTO dto) {
    Long tvShowId = Util.validateTSID(tvShowIdStr);
    TVShow tvShow = tvShowRepository.findById(tvShowId)
        .orElseThrow(ResourceNotFoundException::new);
    Episode episode = new Episode(tvShow, dto.seasonNumber(), dto.episodeNumber(),
        dto.absoluteNumber(), dto.airDate(), dto.runtime());
    episodeRepository.save(episode);
    return EpisodeDTO.from(episode);
  }
}
