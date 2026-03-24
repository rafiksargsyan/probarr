package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.Util;
import com.rsargsyan.probarr.main_ctx.core.app.dto.SeasonDTO;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Season;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.TVShow;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.SeasonRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.TVShowRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeasonService {

  private final SeasonRepository seasonRepository;
  private final TVShowRepository tvShowRepository;

  @Autowired
  public SeasonService(SeasonRepository seasonRepository, TVShowRepository tvShowRepository) {
    this.seasonRepository = seasonRepository;
    this.tvShowRepository = tvShowRepository;
  }

  public List<SeasonDTO> listSeasons(String tvShowIdStr) {
    Long tvShowId = Util.validateTSID(tvShowIdStr);
    return seasonRepository.findByTvShowId(tvShowId).stream()
        .map(SeasonDTO::from)
        .toList();
  }

  @Transactional
  public SeasonDTO createSeason(String tvShowIdStr, SeasonDTO dto) {
    Long tvShowId = Util.validateTSID(tvShowIdStr);
    TVShow tvShow = tvShowRepository.findById(tvShowId)
        .orElseThrow(ResourceNotFoundException::new);
    Season season = new Season(tvShow, dto.seasonNumber(), dto.originalName(), dto.airDate());
    seasonRepository.save(season);
    return SeasonDTO.from(season);
  }
}
