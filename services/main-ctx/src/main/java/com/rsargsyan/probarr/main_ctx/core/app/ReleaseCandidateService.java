package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.Util;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ReleaseCandidateCreationDTO;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ReleaseCandidateDTO;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.ReleaseCandidate;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.CandidateStatus;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.ReleaseCandidateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReleaseCandidateService {

  private final ReleaseCandidateRepository releaseCandidateRepository;

  @Autowired
  public ReleaseCandidateService(ReleaseCandidateRepository releaseCandidateRepository) {
    this.releaseCandidateRepository = releaseCandidateRepository;
  }

  @Transactional
  public ReleaseCandidateDTO createCandidate(ReleaseCandidateCreationDTO dto) {
    Long movieId = dto.movieId() != null ? Util.validateTSID(dto.movieId()) : null;
    Long episodeId = dto.episodeId() != null ? Util.validateTSID(dto.episodeId()) : null;
    ReleaseCandidate candidate = new ReleaseCandidate(movieId, episodeId, dto.name(),
        dto.source(), dto.tracker());
    releaseCandidateRepository.save(candidate);
    return ReleaseCandidateDTO.from(candidate);
  }

  @Transactional
  public ReleaseCandidateDTO updateStatus(String idStr, CandidateStatus status) {
    Long id = Util.validateTSID(idStr);
    ReleaseCandidate candidate = releaseCandidateRepository.findById(id)
        .orElseThrow(ResourceNotFoundException::new);
    candidate.setStatus(status);
    releaseCandidateRepository.save(candidate);
    return ReleaseCandidateDTO.from(candidate);
  }

  public List<ReleaseCandidateDTO> listByMovie(String movieIdStr) {
    Long movieId = Util.validateTSID(movieIdStr);
    return releaseCandidateRepository.findByMovieId(movieId).stream()
        .map(ReleaseCandidateDTO::from)
        .toList();
  }

  public List<ReleaseCandidateDTO> listByEpisode(String episodeIdStr) {
    Long episodeId = Util.validateTSID(episodeIdStr);
    return releaseCandidateRepository.findByEpisodeId(episodeId).stream()
        .map(ReleaseCandidateDTO::from)
        .toList();
  }
}
