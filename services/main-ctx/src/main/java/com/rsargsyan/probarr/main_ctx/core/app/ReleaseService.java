package com.rsargsyan.probarr.main_ctx.core.app;

import com.rsargsyan.probarr.main_ctx.core.Util;
import com.rsargsyan.probarr.main_ctx.core.app.dto.ReleaseDTO;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.Release;
import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.ReleaseCandidate;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.AudioTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.SubtitleTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.CandidateStatus;
import com.rsargsyan.probarr.main_ctx.core.exception.CandidateAlreadyIndexedException;
import com.rsargsyan.probarr.main_ctx.core.exception.ResourceNotFoundException;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.ReleaseCandidateRepository;
import com.rsargsyan.probarr.main_ctx.core.ports.repository.ReleaseRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReleaseService {

  private final ReleaseRepository releaseRepository;
  private final ReleaseCandidateRepository releaseCandidateRepository;

  @Autowired
  public ReleaseService(ReleaseRepository releaseRepository,
                        ReleaseCandidateRepository releaseCandidateRepository) {
    this.releaseRepository = releaseRepository;
    this.releaseCandidateRepository = releaseCandidateRepository;
  }

  @Transactional
  public ReleaseDTO createRelease(String candidateIdStr, ReleaseDTO dto) {
    Long candidateId = Util.validateTSID(candidateIdStr);
    ReleaseCandidate candidate = releaseCandidateRepository.findById(candidateId)
        .orElseThrow(ResourceNotFoundException::new);

    if (releaseRepository.findByCandidateId(candidateId).isPresent()) {
      throw new CandidateAlreadyIndexedException();
    }

    List<AudioTrack> audioTracks = dto.audioTracks() != null
        ? dto.audioTracks().stream().map(a -> a.toEntity()).toList()
        : List.of();
    List<SubtitleTrack> subtitleTracks = dto.subtitleTracks() != null
        ? dto.subtitleTracks().stream().map(s -> s.toEntity()).toList()
        : List.of();

    Release release = new Release(candidate, dto.filePath(), dto.fileSizeBytes(),
        dto.videoCodec(), dto.resolution(), dto.ripType(), dto.runtimeSeconds(),
        audioTracks, subtitleTracks);
    releaseRepository.save(release);

    candidate.setStatus(CandidateStatus.INDEXED);
    releaseCandidateRepository.save(candidate);

    return ReleaseDTO.from(release);
  }

  public ReleaseDTO getRelease(String candidateIdStr) {
    Long candidateId = Util.validateTSID(candidateIdStr);
    Release release = releaseRepository.findByCandidateId(candidateId)
        .orElseThrow(ResourceNotFoundException::new);
    return ReleaseDTO.from(release);
  }
}
