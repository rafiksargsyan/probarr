package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.Util;
import com.rsargsyan.probarr.main_ctx.core.app.dto.TVShowCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.TVShowDTO;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.TVShow;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.TVShowRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TVShowService {

  private final TVShowRepository tvShowRepository;

  @Autowired
  public TVShowService(TVShowRepository tvShowRepository) {
    this.tvShowRepository = tvShowRepository;
  }

  public Page<TVShowDTO> listTVShows(Pageable pageable) {
    return tvShowRepository.findAll(pageable).map(TVShowDTO::from);
  }

  public TVShowDTO getTVShow(String idStr) {
    Long id = Util.validateTSID(idStr);
    return TVShowDTO.from(tvShowRepository.findById(id).orElseThrow(ResourceNotFoundException::new));
  }

  @Transactional
  public TVShowDTO createTVShow(TVShowCreationDTO dto) {
    TVShow tvShow = new TVShow(dto.originalTitle(), dto.originalLocale(), dto.tmdbId(),
        dto.imdbId(), dto.tvdbId(), dto.releaseDate(), dto.useTvdb());
    tvShowRepository.save(tvShow);
    return TVShowDTO.from(tvShow);
  }
}
