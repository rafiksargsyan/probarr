package com.rsargsyan.probarr.main_ctx.core.app.dto;

import com.rsargsyan.probarr.main_ctx.core.domain.aggregate.ReleaseCandidate;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.CandidateSource;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.CandidateStatus;
import io.hypersistence.tsid.TSID;

import java.time.Instant;

public record ReleaseCandidateDTO(
    String id,
    String movieId,
    String episodeId,
    String name,
    String infoHash,
    Long sizeBytes,
    CandidateSource source,
    CandidateStatus status,
    String tracker,
    Instant createdAt
) {
  public static ReleaseCandidateDTO from(ReleaseCandidate candidate) {
    return new ReleaseCandidateDTO(
        candidate.getStrId(),
        candidate.getMovieId() != null ? TSID.from(candidate.getMovieId()).toString() : null,
        candidate.getEpisodeId() != null ? TSID.from(candidate.getEpisodeId()).toString() : null,
        candidate.getName(),
        candidate.getInfoHash(),
        candidate.getSizeBytes(),
        candidate.getSource(),
        candidate.getStatus(),
        candidate.getTracker(),
        candidate.getCreatedAt()
    );
  }
}
