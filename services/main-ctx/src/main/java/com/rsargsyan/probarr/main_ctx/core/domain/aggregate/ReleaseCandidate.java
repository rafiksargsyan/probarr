package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.CandidateSource;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.CandidateStatus;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "release_candidate")
public class ReleaseCandidate extends AggregateRoot {

  @Getter
  @Column(name = "movie_id")
  private Long movieId;

  @Getter
  @Column(name = "episode_id")
  private Long episodeId;

  @Getter
  @Column(nullable = false)
  private String name; // torrent/file name

  @Getter
  private String infoHash; // torrent info hash

  @Getter
  private Long sizeBytes;

  @Getter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CandidateSource source;

  @Getter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CandidateStatus status;

  @Getter
  private String tracker;

  @SuppressWarnings("unused")
  ReleaseCandidate() {}

  public ReleaseCandidate(Long movieId, Long episodeId, String name, CandidateSource source,
                          String tracker) {
    if (movieId == null && episodeId == null) {
      throw new IllegalArgumentException("At least one of movieId or episodeId must be non-null");
    }
    this.movieId = movieId;
    this.episodeId = episodeId;
    this.name = name;
    this.source = source;
    this.status = CandidateStatus.PENDING;
    this.tracker = tracker;
  }

  public void setStatus(CandidateStatus status) {
    this.status = status;
    touch();
  }

  public void setInfoHash(String infoHash) {
    this.infoHash = infoHash;
    touch();
  }

  public void setSizeBytes(Long sizeBytes) {
    this.sizeBytes = sizeBytes;
    touch();
  }

  public void setTracker(String tracker) {
    this.tracker = tracker;
    touch();
  }
}
